#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

Help() {
	# Display Help
	echo "Deploy Helm charts into k8s using argocd"
	echo
	echo "Syntax: apply-helm-charts.sh [-k/--kubectlexec KUBECTLCOMMAND|-h/--help]"
	echo "options:"
	echo "-k/--kubectlexec       Kubectl command to run to interact with k8s instance"
	echo "-h/--help              Print this help text."
	echo
}

POSITIONAL_ARGS=()

while [[ $# -gt 0 ]]; do
	case $1 in
	-k | --kubectlexec)
		KUBECTLCOMMAND="$2"
		shift # past argument
		shift # past value
		;;
	-h | --help)
		Help
		exit
		;;
	-* | --*)
		echo "Unknown option $1"
		Help
		exit 1
		;;
	*)
		POSITIONAL_ARGS+=("$1") # save positional arg
		shift                   # past argument
		;;
	esac
done

set -- "${POSITIONAL_ARGS[@]}" # restore positional parameters

if [[ -n $1 ]]; then
	echo "Last line of file specified as non-opt/last argument:"
	tail -1 "$1"
fi

if [ -z ${KUBECTLCOMMAND+x} ]; then
	echo "-k/--kubectlexec is not set, exiting.";
	exit;
fi

if ! command -v jq &>/dev/null; then
	echo "jq could not be found"
	exit
fi

echo "--------------------------------------"
echo "deploying gittea via helm"
echo "--------------------------------------"

# Note - cannot apply to a directory as ordering of helm-charts matters

# Local Git Serverr for managing state
# kubectl apply --filename helm/gitea.yaml

echo "--------------------------------------"
echo "deploying argo-cd via helm"
echo "--------------------------------------"

# ArgoCD to manage all other components
${KUBECTLCOMMAND} apply --filename helm/argocd.yaml

echo "--------------------------------------"
echo "waiting for argo-cd resources to be ready"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace argocd --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=argo-cd" -l "app.kubernetes.io/component=server"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done

ARGOCD_PASSWORD=$(${KUBECTLCOMMAND} get secret --namespace argocd argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 --decode)

echo "--------------------------------------"
echo "argocd access credentials"
echo "username: admin"
echo "password: ${ARGOCD_PASSWORD}"
echo "http://localhost:30443"
echo "--------------------------------------"

echo "--------------------------------------"
echo "creating kubernetres secret for github ssh key"
echo "--------------------------------------"

PRIVATE_KEYFILE=$(sk --header="Select your private key file for github - searching in ~/.ssh" -c "ls ~/.ssh/")
cp ~/.ssh/${PRIVATE_KEYFILE} ${SCRIPT_DIR}/helm/repo-secret-dev-deployments/sshPrivateKey
cp ~/.ssh/${PRIVATE_KEYFILE} ${SCRIPT_DIR}/helm/repo-secret-helm-charts/sshPrivateKey
${KUBECTLCOMMAND} kustomize ${SCRIPT_DIR}/helm/repo-secret-dev-deployments/ | ${KUBECTLCOMMAND} apply --filename -
${KUBECTLCOMMAND} kustomize ${SCRIPT_DIR}/helm/repo-secret-helm-charts/ | ${KUBECTLCOMMAND} apply --filename -
rm ${SCRIPT_DIR}/helm/repo-secret-dev-deployments/sshPrivateKey
rm ${SCRIPT_DIR}/helm/repo-secret-helm-charts/sshPrivateKey

echo "--------------------------------------"
echo "checking ArgoCD github repo status"
echo "--------------------------------------"

argocd login localhost:30443 --username admin --password ${ARGOCD_PASSWORD} --insecure --plaintext

REPO_STATUS=$(argocd repo get git@github.com:input-output-hk/atala-prism-helm-charts.git -o json | jq -r .connectionState.status)
REPO_MESSAGE=$(argocd repo get git@github.com:input-output-hk/atala-prism-helm-charts.git -o json | jq -r .connectionState.message)
if [[ "${REPO_STATUS}" == "Failed" ]]; then
	echo "Repository did not correctly sync. Error message:"
	echo "${REPO_MESSAGE}"
	echo "Please check your private key in argocd.yaml"
	echo "Deleting repository resource so that it can be recreated"
	${KUBECTLCOMMAND} delete --filename helm/root-prism-platform-repo-secret.yaml
	exit;
fi

echo "--------------------------------------"
echo "deploying platform-root app-of-apps"
echo "--------------------------------------"

${KUBECTLCOMMAND} apply --filename helm/root-prism-platform-app.yaml

echo "--------------------------------------"
echo "waiting for platform-root applications"
echo "--------------------------------------"

echo "--------------------------------------"
echo "waiting for external-secrets"
echo "--------------------------------------"

# Future improvement - check ArgoCD health directly with following:
# For now - checking that a pod in each deployment is in ready state is good enough
# ES_ARGOCD_STATUS=$(argocd app list -o json | jq -r -c '.[] | select(.metadata.name | contains("external-secrets")) | .status.health.status')

while ! ${KUBECTLCOMMAND} wait --namespace external-secrets --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=external-secrets"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done


echo "--------------------------------------"
echo "waiting for apisix"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace ingress-apisix --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=ingress-controller"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for grafana"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace monitoring --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=grafana"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for loki-stack"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace monitoring --for=condition=Ready --timeout=-1s pod -l "name=loki-stack"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for postgres-operator"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace postgres-operator --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=postgres-operator"; do
	echo "Resources do not exist yet and cannot use ${KUBECTLCOMMAND} wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "Setting up local routing"
echo "--------------------------------------"

${KUBECTLCOMMAND} apply --filename helm/routes/argocd-route.yaml
${KUBECTLCOMMAND} apply --filename helm/routes/grafana-route.yaml
# Disable gitea route as currently not deployed in script
# ${KUBECTLCOMMAND} apply --filename helm/routes/gitea-route.yaml

echo "--------------------------------------"
echo "argocd access credentials"
echo "username: admin"
echo "password: ${ARGOCD_PASSWORD}"
echo "http://argocd.local-prism"
echo "--------------------------------------"

GRAFANA_PASSWORD=$(${KUBECTLCOMMAND} get secret --namespace monitoring kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode)

echo "--------------------------------------"
echo "grafana access credentials"
echo "username: admin"
echo "password: ${GRAFANA_PASSWORD}"
echo "http://grafana.local-prism"
echo "--------------------------------------"

echo "--------------------------------------"
echo "Set up complete"
echo "--------------------------------------"
