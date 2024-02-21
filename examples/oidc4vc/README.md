# Local setup

## Run OIDC4VC issuer services

1. cd to `examples/oidc4vc` directory
2. `docker-compose build`
3. `docker-compose up -d`

Alice is a tenant in OEA acting as issuer.
Alice has an existing CIAM and she wants to issue a VC for some of her users.

## Run python holder script

Make sure `python` is install together with these dependencies:

- `requests`

1. `python holder.py`

