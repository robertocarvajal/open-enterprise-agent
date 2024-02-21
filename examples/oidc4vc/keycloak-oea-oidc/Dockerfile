FROM quay.io/keycloak/keycloak:23.0.6
ADD ./target/keycloak-oea-oidc-0.1.0-SNAPSHOT.jar /opt/keycloak/providers/keycloak-oea-oidc-0.1.0-SNAPSHOT.jar
RUN /opt/keycloak/bin/kc.sh build
ENTRYPOINT [ "/opt/keycloak/bin/kc.sh" ]