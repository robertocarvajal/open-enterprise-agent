package io.iohk.atala.oidcvc.controller

import io.iohk.atala.agent.walletapi.model.BaseEntity
import io.iohk.atala.iam.authentication.Authenticator
import io.iohk.atala.iam.authentication.Authorizer
import io.iohk.atala.iam.authentication.DefaultAuthenticator
import io.iohk.atala.iam.authentication.SecurityLogic
import sttp.tapir.ztapir.*
import zio.*

class Oidc4vcServerEndpoints(
    authenticator: Authenticator[BaseEntity],
    authorizer: Authorizer[BaseEntity]
) {

  private val createAuthServerServerEndpoint: ZServerEndpoint[Any, Any] =
    Oidc4vcEndpoints.createAuthServer
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac => _ =>
        ZIO.dieMessage("TODO") // TODO: implement
      }

  private val createCredentialOfferServerEndpoint: ZServerEndpoint[Any, Any] =
    Oidc4vcEndpoints.createCredentialOffer
      .zServerSecurityLogic(SecurityLogic.authorizeWalletAccessWith(_)(authenticator, authorizer))
      .serverLogic { wac => _ =>
        ZIO.dieMessage("TODO") // TODO: implement
      }

  private val getIssuerMetadataServerEndpoint: ZServerEndpoint[Any, Any] =
    Oidc4vcEndpoints.getIssuerMetadata
      .serverLogic { _ =>
        ZIO.dieMessage("TODO") // TODO: implement
      }

  val all: List[ZServerEndpoint[Any, Any]] = List(
    createAuthServerServerEndpoint,
    createCredentialOfferServerEndpoint,
    getIssuerMetadataServerEndpoint
  )
}

object Oidc4vcServerEndpoints {
  def all: URIO[DefaultAuthenticator, List[ZServerEndpoint[Any, Any]]] =
    for {
      authenticator <- ZIO.service[DefaultAuthenticator]
    } yield Oidc4vcServerEndpoints(authenticator, authenticator).all
}
