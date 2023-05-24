#!/usr/bin/env bash

set -e

SCRIPT_DIR=$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &>/dev/null && pwd)

echo "--------------------------------------"
echo "Updating all helm-charts"
echo "--------------------------------------"

# Note - cannot apply to a directory as ordering of helm-charts matters

k3s kubectl --namespace kube-system delete helmchart argocd
k3s kubectl --namespace kube-system delete helmchart gitea
