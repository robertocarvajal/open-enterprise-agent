package io.iohk.atala.agent.server

import io.iohk.atala.agent.server.config.AppConfig
import io.iohk.atala.agent.walletapi.crypto.ApolloSpecHelper
import io.iohk.atala.agent.walletapi.service.EntityServiceImpl
import io.iohk.atala.agent.walletapi.service.WalletManagementService
import io.iohk.atala.agent.walletapi.service.WalletManagementServiceImpl
import io.iohk.atala.agent.walletapi.sql.JdbcEntityRepository
import io.iohk.atala.agent.walletapi.sql.JdbcWalletNonSecretStorage
import io.iohk.atala.agent.walletapi.sql.JdbcWalletSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.iam.authentication.apikey.ApiKeyAuthenticatorImpl
import io.iohk.atala.iam.authentication.apikey.JdbcAuthenticationRepository
import io.iohk.atala.shared.models.WalletAccessContext
import io.iohk.atala.shared.models.WalletAdministrationContext
import io.iohk.atala.shared.models.WalletId
import io.iohk.atala.sharedtest.containers.PostgresTestContainerSupport
import io.iohk.atala.test.container.DBTestUtils
import zio.*
import zio.test.*
import zio.test.Assertion.*
import zio.test.ZIOSpecDefault

import java.net.URL
import io.iohk.atala.agent.server.config.SecretStorageBackend

object AgentInitializationSpec extends ZIOSpecDefault, PostgresTestContainerSupport, ApolloSpecHelper {

  override def spec = {
    val s = suite("AgentInitialization")(
      validateAppConfigSpec,
      initializeDefaultWalletSpec
    ) @@ TestAspect.before(DBTestUtils.runMigrationAgentDB)

    s.provide(
      SystemModule.configLayer,
      ZLayer.fromFunction((appConfig: AppConfig) => appConfig.agent.authentication.apiKey),
      WalletManagementServiceImpl.layer,
      ApiKeyAuthenticatorImpl.layer,
      EntityServiceImpl.layer,
      JdbcWalletNonSecretStorage.layer,
      JdbcWalletSecretStorage.layer,
      JdbcAuthenticationRepository.layer,
      JdbcEntityRepository.layer,
      contextAwareTransactorLayer,
      systemTransactorLayer,
      apolloLayer,
      pgContainerLayer,
      ZLayer.succeed(WalletAdministrationContext.Admin())
    ).provide(Runtime.removeDefaultLoggers)
  }

  private val validateAppConfigSpec = suite("validateAppConfig")(
    test("do not fail when the config is valid")(
      for {
        _ <- AgentInitialization.run.overrideConfig()
      } yield assertCompletes
    ),
    test("do not fail the default wallet is disabled, but any authentication is enabled") {
      for {
        _ <- AgentInitialization.run
          .overrideConfig(enableDefaultWallet = false, enableApiKey = true, enableKeycloak = false)
        _ <- AgentInitialization.run
          .overrideConfig(enableDefaultWallet = false, enableApiKey = false, enableKeycloak = true)
      } yield assertCompletes
    },
    test("fail when the default wallet is disabled and authentication is set to apiKey but disabled")(
      for {
        exit <- AgentInitialization.run
          .overrideConfig(
            enableDefaultWallet = false,
            enableApiKey = false,
            enableKeycloak = false
          )
          .exit
      } yield assert(exit)(fails(isSubtype[RuntimeException](anything)))
    )
  )

  private val initializeDefaultWalletSpec = suite("initializeDefaultWallet")(
    test("do not create default wallet if disabled") {
      for {
        _ <- AgentInitialization.run.overrideConfig(enableDefaultWallet = false)
        wallets <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
      } yield assert(wallets)(isEmpty)
    },
    test("create default wallet if enabled") {
      for {
        _ <- AgentInitialization.run.overrideConfig()
        wallets <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
      } yield assert(wallets)(hasSize(equalTo(1))) &&
        assert(wallets.headOption.map(_.id))(isSome(equalTo(WalletId.default)))
    },
    test("do not recreate default wallet if already exist") {
      for {
        _ <- AgentInitialization.run.overrideConfig()
        wallets1 <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
        _ <- AgentInitialization.run.overrideConfig()
        wallets2 <- ZIO.serviceWithZIO[WalletManagementService](_.listWallets()).map(_._1)
      } yield assert(wallets1)(hasSize(equalTo(1))) && assert(wallets1)(equalTo(wallets2))
    },
    test("create wallet with provided seed") {
      for {
        _ <- AgentInitialization.run.overrideConfig(seed = Some("0" * 128))
        actualSeed <- ZIO
          .serviceWithZIO[WalletSecretStorage](
            _.getWalletSeed
              .provide(ZLayer.succeed(WalletAccessContext(WalletId.default)))
          )
      } yield assert(actualSeed.get.toByteArray)(equalTo(Array.fill[Byte](64)(0)))
    },
    test("create wallet with provided webhook") {
      val url = "http://example.com"
      for {
        _ <- AgentInitialization.run.overrideConfig(webhookUrl = Some(URL(url)))
        webhooks <- ZIO
          .serviceWithZIO[WalletNonSecretStorage](
            _.walletNotification
              .provide(ZLayer.succeed(WalletAccessContext(WalletId.default)))
          )
      } yield assert(webhooks.head.url.toString)(equalTo(url)) &&
        assert(webhooks.head.customHeaders)(isEmpty) &&
        assert(webhooks)(hasSize(equalTo(1)))
    },
    test("create wallet with provided webhook and apikey") {
      val url = "http://example.com"
      val apiKey = "secret"
      for {
        _ <- AgentInitialization.run.overrideConfig(webhookUrl = Some(URL(url)), webhookApiKey = Some(apiKey))
        webhooks <- ZIO
          .serviceWithZIO[WalletNonSecretStorage](
            _.walletNotification
              .provide(ZLayer.succeed(WalletAccessContext(WalletId.default)))
          )
      } yield assert(webhooks.head.url.toString)(equalTo(url)) &&
        assert(webhooks.head.customHeaders.values)(contains(s"Bearer $apiKey")) &&
        assert(webhooks)(hasSize(equalTo(1)))
    }
  )

  extension [R <: AppConfig, E, A](effect: ZIO[R, E, A]) {
    def overrideConfig(
        enableDefaultWallet: Boolean = true,
        seed: Option[String] = None,
        webhookUrl: Option[URL] = None,
        webhookApiKey: Option[String] = None,
        enableApiKey: Boolean = true,
        enableKeycloak: Boolean = false,
    ): ZIO[R, E, A] = {
      import monocle.syntax.all.*
      for {
        appConfig <- ZIO.serviceWith[AppConfig](
          _.focus(_.agent.authentication.apiKey.enabled)
            .replace(enableApiKey)
            .focus(_.agent.authentication.keycloak.enabled)
            .replace(enableKeycloak)
            .focus(_.agent.secretStorage.backend)
            .replace(SecretStorageBackend.postgres)
            .focus(_.agent.defaultWallet.enabled)
            .replace(enableDefaultWallet)
            .focus(_.agent.defaultWallet.seed)
            .replace(seed)
            .focus(_.agent.defaultWallet.webhookUrl)
            .replace(webhookUrl)
            .focus(_.agent.defaultWallet.webhookApiKey)
            .replace(webhookApiKey)
        )
        result <- effect.provideSomeLayer(ZLayer.succeed(appConfig))
      } yield result
    }
  }

}
