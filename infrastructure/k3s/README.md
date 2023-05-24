# Using k3s

This folder contains everything needed to run a full local development environment using Kubernetes which matches the running configuration of a production cluster.

This works by installing additional helm charts onto Kubernetes as it starts up

k3s is the distribution of choice for kubernetes

The helm-controller and AddOns capability is leveraged to configure all the additonal operators and capability akin to a production cluster

Information about using helm with k3 [here](https://docs.k3s.io/helm)

https://blakecovarrubias.com/blog/exploring-the-helmchart-custom-resource-in-k3s/

# Install

:::caution

Requires sudo or root privileges to install

:::

## Using k3s command line tools

Full installation instructions can be found [here](https://docs.k3s.io/quick-start)

```
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" INSTALL_K3S_EXEC="--disable=traefik" sh -
```

## Setting environment variables

```
echo "export KUBECONFIG=/etc/rancher/k3s/k3s.yaml" >> .envrc
direnv allow
```

## Install k9s

Full installation instructions can be found [here](https://k9scli.io/topics/install/)

## Deploying monitoring stack

Full installation instructions can be found [here](https://fabianlee.org/2022/07/02/prometheus-installing-kube-prometheus-stack-on-k3s-cluster/)

```
k3s kubectl apply --filename prometheus-stack.yaml
```

## Deploy APISIX

## Manually manage helm chart

```
k3s kubectl apply --filename apisix.yaml
```

```
k3s kubectl --namespace kube-system delete helm_chart apisix
```

### With Calico and Istio

Full installation instructions can be found [here](https://docs.tigera.io/calico/latest/getting-started/kubernetes/k3s/quickstart)

```
curl -sfL https://get.k3s.io | K3S_KUBECONFIG_MODE="644" INSTALL_K3S_EXEC="--flannel-backend=none --cluster-cidr=192.168.0.0/16 --disable-network-policy --disable=traefik" sh -
```


## Using Rancher Desktop