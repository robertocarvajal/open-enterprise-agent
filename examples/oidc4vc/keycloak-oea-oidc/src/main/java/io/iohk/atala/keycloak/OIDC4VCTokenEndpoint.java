package io.iohk.atala.keycloak;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

import java.util.function.Function;

public class OIDC4VCTokenEndpoint extends TokenEndpoint {
    private static final Logger logger = Logger.getLogger(OIDC4VCTokenEndpoint.class);

    private OEAClient oeaClient;

    public OIDC4VCTokenEndpoint(KeycloakSession session, TokenManager tokenManager, EventBuilder event) {
        super(session, tokenManager, event);
        this.oeaClient = new OEAClient();
    }

    @Override
    public Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code, Function<TokenManager.AccessTokenResponseBuilder, ClientPolicyContext> clientPolicyContextGenerator) {
        if (code) {
            // Normal token operation
            Response originalResponse = super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, clientPolicyContextGenerator);

            // Sync data to Credential Issuer
            String noteKey = AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + OIDC4VCConstants.ISSUER_STATE;
            String issuerState = clientSessionCtx.getClientSession().getNote(noteKey);
            logger.warn("TokenEndpoint issuer_state: " + issuerState);
            oeaClient.syncTokenDetails(issuerState);

            // Modify TokenResponse
            AccessTokenResponse responseEntity = (AccessTokenResponse) originalResponse.getEntity();
            responseEntity.setOtherClaims(OIDC4VCConstants.C_NONCE, "yet-another-nonce");
            responseEntity.setOtherClaims(OIDC4VCConstants.C_NONCE_EXPIRE, 86400); // FIXME hardcoded expiration
            return Response.fromResponse(originalResponse)
                    .entity(responseEntity)
                    .build();
        } else {
            return super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, false, clientPolicyContextGenerator);
        }
    }
}
