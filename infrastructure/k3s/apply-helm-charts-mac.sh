#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

if ! command -v jq &>/dev/null; then
	echo "jq could not be found"
	exit
fi

echo "--------------------------------------"
echo "deploying gittea via helm"
echo "--------------------------------------"

# Note - cannot apply to a directory as ordering of helm-charts matters

# # Local Git Serverr for managing state
kubectl apply --filename helm/gitea.yaml

echo "--------------------------------------"
echo "deploying argo-cd via helm"
echo "--------------------------------------"

# ArgoCD to manage all other components
kubectl apply --filename helm/argocd.yaml

echo "--------------------------------------"
echo "waiting for argo-cd resources to be ready"
echo "--------------------------------------"

while ! kubectl wait --namespace argocd --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=argo-cd" -l "app.kubernetes.io/component=server"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

ARGOCD_PASSWORD=$(kubectl get secret --namespace argocd argocd-initial-admin-secret -o jsonpath="{.data.password}" | base64 --decode)

echo "--------------------------------------"
echo "argocd access credentials"
echo "username: admin"
echo "password: ${ARGOCD_PASSWORD}"
echo "http://localhost/argocd/"
echo "--------------------------------------"

echo "--------------------------------------"
echo "setting up ArgoCD git hub repository"
echo "--------------------------------------"

kubectl apply --filename helm/root-prism-platform-repo-secret.yaml

echo "--------------------------------------"
echo "deploying platform-root app-of-apps"
echo "--------------------------------------"

kubectl apply --filename helm/root-prism-platform-app.yaml

echo "--------------------------------------"
echo "waiting for platform-root applications"
echo "--------------------------------------"

echo "--------------------------------------"
echo "waiting for external-secrets"
echo "--------------------------------------"

while ! kubectl wait --namespace external-secrets --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=external-secrets"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for apisix"
echo "--------------------------------------"

while ! kubectl wait --namespace ingress-apisix --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=ingress-controller"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for grafana"
echo "--------------------------------------"

while ! kubectl wait --namespace monitoring --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=grafana"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for loki-stack"
echo "--------------------------------------"

while ! kubectl wait --namespace monitoring --for=condition=Ready --timeout=-1s pod -l "name=loki-stack"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "waiting for postgres-operator"
echo "--------------------------------------"

while ! kubectl wait --namespace postgres-operator --for=condition=Ready --timeout=-1s pod -l "app.kubernetes.io/name=postgres-operator"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "Setting up local routing"
echo "--------------------------------------"

kubectl apply --filename helm/routes/argocd-route.yaml
kubectl apply --filename helm/routes/grafana-route.yaml
kubectl apply --filename helm/routes/gitea-route.yaml

echo "--------------------------------------"
echo "argocd access credentials"
echo "username: admin"
echo "password: ${ARGOCD_PASSWORD}"
echo "http://localhost/argocd/"
echo "--------------------------------------"

GRAFANA_PASSWORD=$(kubectl get secret --namespace monitoring kube-prometheus-stack-grafana -o jsonpath="{.data.admin-password}" | base64 --decode)

echo "--------------------------------------"
echo "grafana access credentials"
echo "username: admin"
echo "password: ${GRAFANA_PASSWORD}"
echo "http://localhost/grafana/"
echo "--------------------------------------"

echo "--------------------------------------"
echo "Set up complete"
echo "--------------------------------------"
