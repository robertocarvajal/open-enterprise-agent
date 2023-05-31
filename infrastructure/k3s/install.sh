#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# test if brew is installed
if ! command -v brew &>/dev/null; then
	echo "brew could not be found - please install brew (mac or linux)"
	echo "https://brew.sh/"
	exit
fi

install_common() {
	# test if jq is installed via brew
	if ! brew list jq &>/dev/null; then
		echo "jq not installed, installing jq"
		brew install jq
	fi

	# test if argocd is installed via brew
	if ! brew list argocd &>/dev/null; then
		echo "argocd not installed, installing argocd"
		brew install argocd
		exit
	fi

	# test if sk is installed via brew
	if ! brew list sk &>/dev/null; then
		echo "sk not installed, installing sk"
		brew install sk
		exit
	fi
}

install_linux() {
	# test if k3s is installed
	if ! command -v k3s &>/dev/null; then
		echo "k3s not installed, installing k3s"
		curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" INSTALL_K3S_EXEC="--disable=traefik" sh -
	fi
	KUBECTLCOMMAND="k3s kubectl"
	# test if k3s is running
	if ! systemctl is-active --quiet k3s; then
		# start k3s
		echo "k3s is not running - starting.."
		sudo systemctl start k3s
		until systemctl is-active --quiet k3s
		do
			echo "waiting for k3s to start"
			sleep 5
		done
	fi
}

install_mac() {
	# test if k3d is installed
	if ! command -v k3d &>/dev/null; then
		echo "k3d not installed, installing k3d"
		brew install k3d@5.5.1
		exit
	fi
	KUBECTLCOMMAND="kubectl"

	# test if k3d has a cluster called local-cluster
	if k3d cluster list | grep -q local-cluster; then
		echo "local-cluster exists, continuing"
		echo "ensure local-cluster is started"
		k3d cluster start local-cluster
	else
		echo "local-cluster does not exist, creating local-cluster"
		k3d cluster create local-cluster --k3s-arg="--disable=traefik@server:0" -p "30443:30443@server:0" -p "30080:30080@server:0"
		k3d kubeconfig merge local-cluster --kubeconfig-switch-context
	fi

}

install_common

if [ "$(uname)" == "Darwin" ]; then
    # Do something under Mac OS X platform
	install_mac
elif [ "$(expr substr $(uname -s) 1 5)" == "Linux" ]; then
    # Do something under GNU/Linux platform
	install_linux
else
	echo "Unsupported OS, exiting"
	exit 0;
fi

if [ -z ${KUBECTLCOMMAND+x} ]; then
	echo "KUBECTLCOMMAND is not set, exiting.";
	exit;
fi

echo "--------------------------------------"
echo "wait for coredns startup"
echo "--------------------------------------"

while ! ${KUBECTLCOMMAND} wait --namespace kube-system --for=condition=Ready --timeout=-1s pod -l "k8s-app=kube-dns"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

echo "--------------------------------------"
echo "modifying hosts file"
echo "--------------------------------------"

# check if host entry for grafana is in host file
if grep -q "grafana.local-prism" /etc/hosts; then
	echo "grafana.local-prism exists, continuing"
else
	echo "grafana.local-prism does not exist, adding grafana.local-prism to /etc/hosts"
	sudo echo "127.0.0.1  grafana.local-prism" | sudo tee -a /etc/hosts
fi

# check if host entry for argocd is in host file
if grep -q "argocd.local-prism" /etc/hosts; then
	echo "argocd.local-prism exists, continuing"
else
	echo "argocd.local-prism does not exist, adding argocd.local-prism to /etc/hosts"
	echo "127.0.0.1  argocd.local-prism" | sudo tee -a /etc/hosts
fi

# check if host entry for gitea is in host file
if grep -q "gitea.local-prism" /etc/hosts; then
	echo "gitea.local-prism exists, continuing"
else
	echo "gitea.local-prism does not exist, adding gitea.local-prism to /etc/hosts"
	echo "127.0.0.1  gitea.local-prism" | sudo tee -a /etc/hosts
fi

# (jq -c . < ~/.docker/config.json | base64 -w 0)
# jq -c . < ~/.docker/config.json | base64 -w 0 | xclip -selection clipboard
# base64 prism-agent-1.1.0.tgz -w 0 | xclip -selection clipboard

(${SCRIPT_DIR}/apply-helm-charts.sh -k "${KUBECTLCOMMAND}")
