# Wait for keycloak ready
GET {{ keycloak_base_url }}/health
[Options]
retry: 60
HTTP 200

# Admin login
POST {{ keycloak_base_url }}/realms/master/protocol/openid-connect/token
[FormParams]
grant_type: password
client_id: admin-cli
username: {{ keycloak_admin_user }}
password: {{ keycloak_admin_password }}
HTTP 200
[Captures]
admin_access_token: jsonpath "$.access_token"

####################################
# Configure realm for OEA
####################################

# Create realm
POST {{ keycloak_base_url }}/admin/realms
authorization: Bearer {{ admin_access_token }}
{
  "realm": "{{ keycloak_realm }}",
  "enabled": true
}
HTTP 201

# Create OEA client
POST {{ keycloak_base_url }}/admin/realms/{{ keycloak_realm }}/clients
Authorization: Bearer {{ admin_access_token }}
{
  "id": "{{ keycloak_client_id }}",
  "authorizationServicesEnabled": true,
  "serviceAccountsEnabled": true,
  "secret": "{{ keycloak_client_secret }}"
}
HTTP 201

# Create Alice, the OEA tenant
POST {{ keycloak_base_url }}/admin/realms/{{ keycloak_realm }}/users
Authorization: Bearer {{ admin_access_token }}
{
  "username": "{{ alice_username }}",
  "firstName": "Alice",
  "lastName": "Wonderland",
  "enabled": true,
  "email": "alice@atalaprism.io",
  "credentials": [{"value": "{{ alice_password }}", "temporary": false}]
}
HTTP 201

####################################
# Configure realm for Alice external OIDC Provider
####################################
# Create Alice's realm
POST {{ keycloak_base_url }}/admin/realms
authorization: Bearer {{ admin_access_token }}
{
  "realm": "{{ alice_keycloak_realm }}",
  "enabled": true
}
HTTP 201

# Pre-register OEA client for token introspection // TODO: dynamic registration?
POST {{ keycloak_base_url }}/admin/realms/{{ alice_keycloak_realm }}/clients
Authorization: Bearer {{ admin_access_token }}
{
  "id": "alice-oea-client",
  "authorizationServicesEnabled": true,
  "serviceAccountsEnabled": true,
  "secret": "1234"
}
HTTP 201

# Pre-register holder wallet client // TODO: dynamic registration?
POST {{ keycloak_base_url }}/admin/realms/{{ alice_keycloak_realm }}/clients
Authorization: Bearer {{ admin_access_token }}
{
  "id": "{{ bob_wallet_client_id }}",
  "publicClient": true,
  "consentRequired": true,
  "redirectUris": [ "http://localhost:5000/*" ]
}
HTTP 201

# Create a scope for issuable credential
POST {{ keycloak_base_url }}/admin/realms/{{ alice_keycloak_realm }}/client-scopes
Authorization: Bearer {{ admin_access_token }}
{
  "name": "UniversityDegreeCredential",
  "description": "The University Degree Credential",
  "protocol": "openid-connect",
  "type": "optional",
  "attributes": {
    "consent.screen.text": "University Degree",
    "display.on.consent.screen": "true",
    "include.in.token.scope": "true",
    "gui.order": ""
  }
}
HTTP 201
[Captures]
client_scope_id: header "Location" split "/" nth 7

# scope mapping
PUT {{ keycloak_base_url }}/admin/realms/{{ alice_keycloak_realm }}/clients/{{ bob_wallet_client_id }}/optional-client-scopes/{{ client_scope_id }}
Authorization: Bearer {{ admin_access_token }}
{}
HTTP 204

# Create Bob in Alice's realm
POST {{ keycloak_base_url }}/admin/realms/{{ alice_keycloak_realm }}/users
Authorization: Bearer {{ admin_access_token }}
{
  "username": "{{ bob_username }}",
  "firstName": "SpongeBob",
  "lastName": "SquarePants",
  "enabled": true,
  "email": "bob@atalaprism.io",
  "credentials": [{"value": "{{ bob_password }}", "temporary": false}]
}
HTTP 201

