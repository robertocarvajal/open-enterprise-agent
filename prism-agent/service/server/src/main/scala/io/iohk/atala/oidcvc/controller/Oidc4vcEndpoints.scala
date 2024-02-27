package io.iohk.atala.oidcvc.controller

import io.iohk.atala.api.http.EndpointOutputs
import io.iohk.atala.api.http.{ErrorResponse, RequestContext}
import io.iohk.atala.iam.authentication.apikey.ApiKeyCredentials
import io.iohk.atala.iam.authentication.apikey.ApiKeyEndpointSecurityLogic.apiKeyHeader
import io.iohk.atala.iam.authentication.oidc.JwtCredentials
import io.iohk.atala.iam.authentication.oidc.JwtSecurityLogic.jwtAuthHeader
import io.iohk.atala.oidcvc.controller.http.CreateAuthorizationServerRequest
import io.iohk.atala.oidcvc.controller.http.CreateCredentialOfferRequest
import io.iohk.atala.oidcvc.controller.http.CreateCredentialOfferResponse
import io.iohk.atala.oidcvc.controller.http.IssuerMetadata
import io.iohk.atala.oidcvc.controller.http.NonceRequest
import io.iohk.atala.oidcvc.controller.http.NonceResponse
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.zio.jsonBody
import sttp.tapir.json.zio.schemaForZioJsonValue
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonEncoder, JsonDecoder}

import java.util.UUID

// TODO: reorganize this
package http {

  final case class CreateAuthorizationServerRequest(url: String)
  object CreateAuthorizationServerRequest {
    given encoder: JsonEncoder[CreateAuthorizationServerRequest] =
      DeriveJsonEncoder.gen[CreateAuthorizationServerRequest]
    given decoder: JsonDecoder[CreateAuthorizationServerRequest] =
      DeriveJsonDecoder.gen[CreateAuthorizationServerRequest]
    given schema: Schema[CreateAuthorizationServerRequest] = Schema.derived
  }

  final case class CreateCredentialOfferRequest(
      issuingDID: Option[String],
      subjectId: Option[String],
      credentialDefinitionId: Option[UUID],
      claims: zio.json.ast.Json,
  )
  object CreateCredentialOfferRequest {
    given encoder: JsonEncoder[CreateCredentialOfferRequest] =
      DeriveJsonEncoder.gen[CreateCredentialOfferRequest]
    given decoder: JsonDecoder[CreateCredentialOfferRequest] =
      DeriveJsonDecoder.gen[CreateCredentialOfferRequest]
    given schema: Schema[CreateCredentialOfferRequest] = Schema.derived
  }

  final case class CreateCredentialOfferResponse(
      credentialOffer: String
  )
  object CreateCredentialOfferResponse {
    given encoder: JsonEncoder[CreateCredentialOfferResponse] =
      DeriveJsonEncoder.gen[CreateCredentialOfferResponse]
    given decoder: JsonDecoder[CreateCredentialOfferResponse] =
      DeriveJsonDecoder.gen[CreateCredentialOfferResponse]
    given schema: Schema[CreateCredentialOfferResponse] = Schema.derived
  }

  final case class IssuerMetadata(
      credential_issuer: String,
      authorization_servers: Seq[String],
      credential_endpoint: String,
      credential_identifiers_supported: Boolean
  )
  object IssuerMetadata {
    given encoder: JsonEncoder[IssuerMetadata] =
      DeriveJsonEncoder.gen[IssuerMetadata]
    given decoder: JsonDecoder[IssuerMetadata] =
      DeriveJsonDecoder.gen[IssuerMetadata]
    given schema: Schema[IssuerMetadata] = Schema.derived
  }

  final case class NonceRequest(issuerState: String)
  object NonceRequest {
    given encoder: JsonEncoder[NonceRequest] =
      DeriveJsonEncoder.gen[NonceRequest]
    given decoder: JsonDecoder[NonceRequest] =
      DeriveJsonDecoder.gen[NonceRequest]
    given schema: Schema[NonceRequest] = Schema.derived
  }

  final case class NonceResponse(nonce: String, nonceExpiresIn: Int)
  object NonceResponse {
    given encoder: JsonEncoder[NonceResponse] =
      DeriveJsonEncoder.gen[NonceResponse]
    given decoder: JsonDecoder[NonceResponse] =
      DeriveJsonDecoder.gen[NonceResponse]
    given schema: Schema[NonceResponse] = Schema.derived
  }

}

object Oidc4vcEndpoints {

  private val tagName = "OIDC4VC"

  private val basePublicEndpoint = endpoint
    .tag(tagName)
    .in("oidc4vc")
    .in(extractFromRequest[RequestContext](RequestContext.apply))

  private val baseIssuerEndpoint = basePublicEndpoint
    .securityIn(apiKeyHeader)
    .securityIn(jwtAuthHeader)

  private val baseHolderEndpoint = basePublicEndpoint
    .in(path[String]("some-issuer-identifier"))

  val createAuthServer: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateAuthorizationServerRequest),
    ErrorResponse,
    Unit,
    Any
  ] =
    baseIssuerEndpoint.post
      .in("authorization-servers")
      .in(jsonBody[CreateAuthorizationServerRequest])
      .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
      .out(statusCode(StatusCode.Created))
      .summary("Issuer adds an AuthorizationServer that will authorize the holder to the credential endpoint.")

  val createCredentialOffer: Endpoint[
    (ApiKeyCredentials, JwtCredentials),
    (RequestContext, CreateCredentialOfferRequest),
    ErrorResponse,
    CreateCredentialOfferResponse,
    Any
  ] = baseIssuerEndpoint.post
    .in("credential-offers")
    .in(jsonBody[CreateCredentialOfferRequest])
    .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
    .out(statusCode(StatusCode.Created))
    .out(jsonBody[CreateCredentialOfferResponse])
    .summary("Issuer creates a credential offer and give the link to holder out-of-band (e.g. QR code)")

  val getIssuerMetadata: Endpoint[
    Unit,
    (RequestContext, String),
    ErrorResponse,
    IssuerMetadata,
    Any
  ] = baseHolderEndpoint
    .in(".well-known" / "openid-credential-issuer")
    .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
    .out(statusCode(StatusCode.Ok))
    .out(jsonBody[IssuerMetadata])
    .summary("OIDC4VC Issuer Metadata")

  val nonce: Endpoint[
    Unit,
    (RequestContext, NonceRequest),
    ErrorResponse,
    NonceResponse,
    Any
  ] =
    basePublicEndpoint.post
      .in("nonce")
      .in(jsonBody[NonceRequest])
      .errorOut(EndpointOutputs.basicFailureAndNotFoundAndForbidden)
      .out(statusCode(StatusCode.Ok))
      .out(jsonBody[NonceResponse])

}
