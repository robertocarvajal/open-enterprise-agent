import json
import requests
import threading
import urllib

from http.server import BaseHTTPRequestHandler, HTTPServer


LOGIN_REDIRECT_URL = "http://localhost:5000/cb"

CREDENTIAL_ISSUER = "http://localhost:8085/prism-agent/oidc4vc/did-or-uuid"
CREDENTIAL_CONFIGURATION_ID = "UniversityDegreeCredential"

BOB_CLIENT_ID = "bob-wallet"


def init_issuer_metadata(credential_issuer: str):
    metadata_url = f"{credential_issuer}/.well-known/openid-credential-issuer"
    # TODO: OEA should return these instead of hardcoded values
    return {
        "credential_issuer": CREDENTIAL_ISSUER,
        "authorization_servers": [
            "http://localhost:9980/realms/external-alice-as",
        ],
        "credential_endpoint": f"{CREDENTIAL_ISSUER}/credentials",
        "credential_identifiers_supported": False,
        "credential_configurations_supported": {
            CREDENTIAL_CONFIGURATION_ID: {
                "format": "jwt_vc_json",
                "scope": "UniversityDegreeCredential",
                "credential_signing_alg_values_supported": ["ES256K"],
                "credential_definition": {
                    "type": ["VerifiableCredential", "UniversityDegreeCredential"],
                    "credentialSubject": {
                        "degree": {},
                        "gpa": {"display": [{"name": "GPA"}]},
                    },
                },
            }
        },
    }


def init_issuer_as_metadata(authorization_server: str):
    metadata_url = f"{authorization_server}/.well-known/openid-configuration"
    response = requests.get(metadata_url)
    metadata = response.json()
    # print(json.dumps(metadata, indent=2))
    return metadata


def start_login_flow(auth_endpoint: str, token_endpoint: str):
    authorization_code = start_authorization_request(auth_endpoint)
    token_response = start_token_request(token_endpoint, authorization_code)
    return token_response


def start_authorization_request(auth_endpoint: str):
    # Authorization Request
    queries = urllib.parse.urlencode(
        {
            "redirect_uri": LOGIN_REDIRECT_URL,
            "response_type": "code",
            "client_id": BOB_CLIENT_ID,
            "scope": "openid " + CREDENTIAL_CONFIGURATION_ID,
            "issuer_state": "i-heard-you-like-state",
        }
    )
    login_url = f"{auth_endpoint}?{queries}"
    print("\n##############################\n")
    print("Open this link in the browser to login\n")
    print(login_url)
    print("\n##############################\n")

    # wait for authorization redirect
    start_redirect_listener()

    global authorzation_code  # hold my beer
    return authorzation_code


def start_token_request(token_endpoint: str, authorization_code: str):
    # Token Request
    response = requests.post(
        token_endpoint,
        data={
            "grant_type": "authorization_code",
            "code": authorization_code,
            "client_id": BOB_CLIENT_ID,
            "redirect_uri": LOGIN_REDIRECT_URL,
        },
    )
    return response.json()


def start_redirect_listener():
    class AuthResponseHandler(BaseHTTPRequestHandler):
        def do_GET(self):
            # Uncomment to log request
            # print(f"Request Line: {self.command} {self.path} HTTP/{self.protocol_version}")
            # for header, value in self.headers.items():
            #     print(f"{header}: {value}")
            # content_length = int(self.headers.get('Content-Length', 0))
            # request_body = self.rfile.read(content_length).decode('utf-8')
            # print(f"Request Body: {request_body}")

            global authorzation_code  # hold my beer
            params = self.path.split("?")[-1]
            params = urllib.parse.parse_qs(params)
            authorzation_code = params["code"][0]

            self.send_response(200)
            self.end_headers()
            self.wfile.write(b"Login successful")

            # Shutdown listener
            print("terminating http listener ...")
            shutdown_thread = threading.Thread(target=self.server.shutdown)
            shutdown_thread.start()

    with HTTPServer(("0.0.0.0", 5000), AuthResponseHandler) as httpd:
        print("wating for authorization redirect ...")
        httpd.serve_forever()
        print("http listener terminated")


if __name__ == "__main__":
    # scan QR code containing CredentialOffer
    queries = urllib.parse.urlencode(
        {
            "credential_offer": {
                "credential_issuer": CREDENTIAL_ISSUER,
                "credential_configuration_ids": CREDENTIAL_CONFIGURATION_ID,
                "grants": {"authorization_code": {}},
            }
        }
    )
    credential_offer_uri = f"openid-credential-offer://?{queries}"

    print("\n##############################\n")
    print(f"QR code scanned, got credential-offer\n\n{credential_offer_uri}\n")
    print("\n##############################\n")

    input("\nEnter to continue ...")

    issuer_metadata = init_issuer_metadata(CREDENTIAL_ISSUER)
    authorzation_server = issuer_metadata["authorization_servers"][0]
    print("\n::::: Issuer Metadata :::::")
    print(json.dumps(issuer_metadata, indent=2))
    input("\nEnter to continue ...")

    issuer_as_metadata = init_issuer_as_metadata(authorzation_server)
    issuer_as_token_endpoint = issuer_as_metadata["token_endpoint"]
    issuer_as_authorization_endpoint = issuer_as_metadata["authorization_endpoint"]
    print("\n::::: Issuer Authorization Server Metadata :::::")
    print(f"issuer_as_auth_endpoint: {issuer_as_authorization_endpoint}")
    print(f"issuer_as_token_endpoint: {issuer_as_token_endpoint}")
    input("\nEnter to continue ...")

    token_response = start_login_flow(
        issuer_as_authorization_endpoint, issuer_as_token_endpoint
    )
    print("::::: Token Response :::::")
    print(json.dumps(token_response, indent=2))
