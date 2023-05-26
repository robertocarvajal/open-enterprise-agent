#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

if ! command -v jq &>/dev/null; then
	echo "jq could not be found"
	exit
fi

echo "--------------------------------------"
echo "wait for coredns startup"
echo "--------------------------------------"

while ! kubectl wait --namespace kube-system --for=condition=Ready --timeout=-1s pod -l "k8s-app=kube-dns"; do
	echo "Resources do not exist yet and cannot use kubectl wait command, sleeping 5 seconds"
	sleep 5
done

# 127.0.0.1  grafana.local-prism                                                                                                                                                              │
# 127.0.0.1  argocd.local-prism                                                                                                                                                               │
# 127.0.0.1  gitea.local-prism

# (jq -c . < ~/.docker/config.json | base64 -w 0)
# jq -c . < ~/.docker/config.json | base64 -w 0 | xclip -selection clipboard
# base64 prism-agent-1.1.0.tgz -w 0 | xclip -selection clipboard

(${SCRIPT_DIR}/apply-helm-charts-mac.sh)
