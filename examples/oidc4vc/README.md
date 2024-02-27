# Local setup

## Run OIDC4VC issuer services

```bash
cd examples/oidc4vc
docker-compose build
docker-compose up
```

Alice is a tenant in OEA acting as issuer.
Alice has an existing customer IAM and she wants to issue a VC for some of her users.

## Run python holder script

Make sure `python` is install together with these dependencies: `requests`

Then run the following command

```bash
python holder.py
```

# Sequence Diagram

```mermaid
sequenceDiagram
    actor Issuer
    actor Holder
    participant OEA
    participant AS as Issuer AS

    autonumber
    Note over Issuer: Issuer registers an AS<br/>if not already have
    Issuer ->>+ OEA: POST /authorization-servers
    OEA ->>- Issuer: AuthorizationServer

    Note over Issuer: Issuer registers a credential definition<br/>if not already have
    Issuer ->>+ OEA: POST /credential-definitions
    OEA ->>+ AS: Register new scope
    OEA ->>- Issuer: CredentialDefinition

    Note over Issuer: Issuer initiate issuance process
    Issuer ->>+ OEA: POST /credential-offers<br/>(claims, subject)
    OEA ->>- Issuer: CredentialOffer<br/>(cred_config_id, issuer_state)
    Issuer ->> Holder: QR Code<br/>(issuer_url, cred_config_id, issuer_state)

    Holder ->>+ OEA: GET /.well-known/openid-credential-issuer
    OEA ->>- Holder: CredentialIssuerMetadata

    Holder ->>+ AS: GET /.well-known/openid-configuration
    AS ->>- Holder: AuthorizationServerMetadata

    Holder ->>+ AS: AuthorizationRequest<br/>(scope, issuer_state)
    Note right of AS: Store issuer_state<br/>in session
    AS ->>- Holder: AuthorizationResponse<br/>(authorization_code)

    Holder ->>+ AS: TokenRequest<br/>(authorization_code)
    Note right of AS: Retrieve issuer_state<br/>from session
    AS ->>+ OEA: POST /nonce<br/>(issuer_state)
    Note over OEA: Correlate nonce<br/>and issuer_state
    OEA ->>- AS: NonceResponse (c_nonce)
    AS ->>- Holder: TokenResponse<br/>(access_token, c_nonce)

    Holder ->>+ OEA: POST /credentials<br/>(format, proof)
    Note over OEA: Correlate c_nonce, issuer_state<br/>to get issued credential
    OEA ->>- Holder: Credential
```
