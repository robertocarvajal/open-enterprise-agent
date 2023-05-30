#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

# test if jq is installed
if ! command -v jq &>/dev/null; then
	echo "jq could not be found"
	exit
fi

# test if brew is installed
if ! command -v brew &>/dev/null; then
	echo "brew could not be found"
	exit
fi

# test if argocd is installed via brew
if ! brew list argocd &>/dev/null; then
	echo "argocd not installed, installing argocd"
	brew install argocd
	exit
fi

# test if k3d is installed
if ! command -v k3d &>/dev/null; then
	echo "k3d not installed, installing k3d"
	wget -q -O - https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | TAG=v5.5.1 bash
	exit
fi

# test if k3d has a cluster called local-cluster
if k3d cluster list | grep -q local-cluster; then
	echo "local-cluster exists, continuing"
else
	echo "local-cluster does not exist, creating local-cluster"
	k3d kubeconfig merge local-cluster --kubeconfig-switch-context
	k3d cluster create local-cluster --k3s-arg="--disable=traefik@server:0"
fi

echo "--------------------------------------"
echo "wait for coredns startup"
echo "--------------------------------------"

while ! kubectl wait --namespace kube-system --for=condition=Ready --timeout=-1s pod -l "k8s-app=kube-dns"; do
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

(${SCRIPT_DIR}/apply-helm-charts-mac.sh)
