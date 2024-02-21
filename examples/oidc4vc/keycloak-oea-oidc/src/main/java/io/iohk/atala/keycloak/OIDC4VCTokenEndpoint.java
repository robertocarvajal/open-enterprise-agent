package io.iohk.atala.keycloak;

import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.ClientSessionContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.endpoints.TokenEndpoint;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.services.clientpolicy.ClientPolicyContext;

import java.util.function.Function;

public class OIDC4VCTokenEndpoint extends TokenEndpoint {
    private static final Logger logger = Logger.getLogger(OIDC4VCTokenEndpoint.class);

    public OIDC4VCTokenEndpoint(KeycloakSession session, TokenManager tokenManager, EventBuilder event) {
        super(session, tokenManager, event);
    }

    @Override
    public Response createTokenResponse(UserModel user, UserSessionModel userSession, ClientSessionContext clientSessionCtx,
                                        String scopeParam, boolean code, Function<TokenManager.AccessTokenResponseBuilder, ClientPolicyContext> clientPolicyContextGenerator) {
        // Only support authorization_code grant for now
        if (code) {
            Response originalResponse = super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, true, clientPolicyContextGenerator);
            AccessTokenResponse responseEntity = (AccessTokenResponse) originalResponse.getEntity();
            responseEntity.setOtherClaims("c_nonce", "yet-another-nonce");
            responseEntity.setOtherClaims("c_nonce_expires_in", 86400);
            return Response.fromResponse(originalResponse)
                    .entity(responseEntity)
                    .build();
        } else {
            return super.createTokenResponse(user, userSession, clientSessionCtx, scopeParam, false, clientPolicyContextGenerator);
        }
    }

}
