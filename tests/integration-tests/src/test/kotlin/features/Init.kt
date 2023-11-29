package features

import com.sksamuel.hoplite.ConfigException
import com.sksamuel.hoplite.ConfigLoader
import common.ListenToEvents
import common.TestConstants
import config.Config
import io.cucumber.java.AfterAll
import io.cucumber.java.BeforeAll
import io.iohk.atala.prism.models.CreateWalletRequest
import io.iohk.atala.prism.models.CreateWebhookNotification
import io.restassured.RestAssured
import io.restassured.builder.RequestSpecBuilder
import net.serenitybdd.screenplay.Actor
import net.serenitybdd.screenplay.actors.Cast
import net.serenitybdd.screenplay.actors.OnStage
import net.serenitybdd.screenplay.rest.abilities.CallAnApi
import org.apache.http.HttpStatus
import java.util.*

val config = ConfigLoader().loadConfigOrThrow<Config>(TestConstants.TESTS_CONFIG)

/**
 * This function starts all services and actors before all tests.
 */
fun initServices() {
    config.services?.keycloak?.start(
        config.roles.filter { it.name != "Admin" }.map { it.name }
    )
    config.services?.prismNode?.start()
    config.services?.vault?.start()
    config.agents?.forEach { agent ->
        agent.start()
    }
}

/**
 * This function initializes all actors and sets the stage.
 */
fun initActors() {
    /**
     * This function initializes a wallet for an actor when Keycloak is used.
     *
     * @param actor The actor for which the wallet should be initialized.
     */
    fun initializeWallet(actor: Actor) {
        val spec = RequestSpecBuilder()
            .setBaseUri(actor.usingAbilityTo(CallAnApi::class.java).resolve("/"))
        if (actor.recall<String>("AUTH_KEY") != null) {
            spec.addHeader(actor.recall("AUTH_HEADER"), actor.recall("AUTH_KEY"))
        }
        if (actor.recall<String>("BEARER_TOKEN") != null) {
            spec.addHeader("Authorization", "Bearer ${actor.recall<String>("BEARER_TOKEN")}")
        }
        RestAssured
            .given().spec(spec.build())
            .body(
                CreateWalletRequest(
                    name = UUID.randomUUID().toString()
                )
            )
            .log().all(true)
            .post("/wallets")
            .then().log().all(true)
            .statusCode(HttpStatus.SC_CREATED)
    }

    /**
     * This function registers a webhook for an actor.
     *
     * @param actor The actor for which the webhook should be registered.
     * @param webhookUrl The url of the webhook.
     */
    fun registerWebhook(actor: Actor, webhookUrl: String) {
        val spec = RequestSpecBuilder()
            .setBaseUri(actor.usingAbilityTo(CallAnApi::class.java).resolve("/"))
        if (actor.recall<String>("AUTH_KEY") != null) {
            spec.addHeader(actor.recall("AUTH_HEADER"), actor.recall("AUTH_KEY"))
        }
        if (actor.recall<String>("BEARER_TOKEN") != null) {
            spec.addHeader("Authorization", "Bearer ${actor.recall<String>("BEARER_TOKEN")}")
        }
        RestAssured
            .given().spec(spec.build())
            .body(CreateWebhookNotification(url = webhookUrl))
            .log().all(true)
            .post("/events/webhooks")
            .then().log().all(true)
            .statusCode(HttpStatus.SC_OK)
    }

    val cast = Cast()
    config.roles.forEach { role ->
        cast.actorNamed(
            role.name,
            CallAnApi.at(role.url.toExternalForm())
        )
    }
    if (true) { //(config.services?.keycloak != null) {
        cast.actors.filter { it.name != "Admin" }.forEach { actor ->
            try {
                actor.remember("BEARER_TOKEN", getKeycloakAuthToken(actor.name, actor.name))
            } catch (e: NullPointerException) {
                throw ConfigException("Keycloak is configured, but no token found for user ${actor.name}!")
            }
        }
    }
    config.roles.filter { it.name != "Admin" }.forEach { role ->
        val actor = cast.actorNamed(role.name)
        if (role.apikey != null) {
            actor.remember("AUTH_KEY", role.apikey)
            actor.remember("AUTH_HEADER", role.authHeader)
        }
        initializeWallet(actor)
        if (role.webhook != null) {
            actor.whoCan(ListenToEvents.at(role.webhook.url, role.webhook.internalPort))
            if (role.webhook.initRequired) {
                registerWebhook(actor, role.webhook.url.toExternalForm())
            }
        }
    }

    OnStage.setTheStage(cast)
}

@BeforeAll
fun init() {
    initServices()
    initActors()
}

@AfterAll
fun clearStage() {
    OnStage.drawTheCurtain()
    config.agents?.forEach { agent ->
        agent.stop()
    }
    config.services?.keycloak?.stop()
    config.services?.prismNode?.stop()
    config.services?.vault?.stop()
}

fun getKeycloakAuthToken(username: String, password: String): String {
    val keycloakUrl = "https://sit-keycloak.atalaprism.io"
    val realm = "prism-agent"
    val clientId = "prism-agent"
    val clientSecret = "nKSmssRpjQe4yecx"

    val tokenResponse =
        RestAssured
            .given().body("grant_type=password&client_id=$clientId&client_secret=$clientSecret&username=$username&password=$password")
            .contentType("application/x-www-form-urlencoded").log().all(true)
            //.header("Host", "localhost")
            .post("$keycloakUrl/realms/$realm/protocol/openid-connect/token")
            .thenReturn()
    tokenResponse.then().statusCode(HttpStatus.SC_OK)
    tokenResponse.then().log().all(true)
    return tokenResponse.body.jsonPath().getString("access_token")
}
