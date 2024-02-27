package io.iohk.atala.keycloak;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.function.Supplier;

public class OEAClient {

    private static final Logger logger = Logger.getLogger(OEAClient.class);
    private static final String oeaBaseUrl = "http://localhost:8085";

    private final Supplier<CloseableHttpClient> httpClient = OEAClient::newCloseableHttpClient;


    public OEAClient() {
    }

    public static CloseableHttpClient newCloseableHttpClient() {
        return HttpClientBuilder.create().build();
    }

    public NonceResponse syncTokenDetails(String issuerState) {
        try (CloseableHttpClient client = httpClient.get()) {
            HttpPost post = new HttpPost(oeaBaseUrl + "/oidc4vc/nonce");
//            post.setHeader("Authorization", "Bearer XYZ");
            post.setEntity(new StringEntity(JsonSerialization.writeValueAsString(new NonceRequest(issuerState)), ContentType.APPLICATION_JSON));
            return NonceResponse.fromResponse(client.execute(post));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static class NonceRequest {
        private String issuerState;

        public NonceRequest() {
            // for reflection
        }

        public NonceRequest(String issuerState) {
            this.issuerState = issuerState;
        }

        public String getIssuerState() {
            return this.issuerState;
        }

        public void setIssuerState(String issuerState) {
            this.issuerState = issuerState;
        }
    }

    public static class NonceResponse {
        private String nonce;
        private int nonceExpiresIn;

        public NonceResponse() {
            // for reflection
        }

        public static NonceResponse fromResponse(CloseableHttpResponse response) throws RuntimeException {
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode == 200) {
                try (response) {
                    HttpEntity entity = response.getEntity();
                    String jsonString = EntityUtils.toString(entity);
                    ObjectMapper objectMapper = new ObjectMapper();
                    return objectMapper.readValue(jsonString, NonceResponse.class);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                throw new RuntimeException("The response status from OEA was not successful: " + statusCode);
            }
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public int getNonceExpiresIn() {
            return nonceExpiresIn;
        }

        public void setNonceExpiresIn(int nonceExpiresIn) {
            this.nonceExpiresIn = nonceExpiresIn;
        }
    }
}
