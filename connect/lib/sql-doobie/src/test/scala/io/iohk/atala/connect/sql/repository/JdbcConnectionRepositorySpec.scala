package io.iohk.atala.connect.sql.repository

import cats.effect.std.Dispatcher
import com.dimafeng.testcontainers.PostgreSQLContainer
import doobie.util.transactor.Transactor
import io.iohk.atala.connect.core.repository.{ConnectionRepository, ConnectionRepositorySpecSuite}
import io.iohk.atala.shared.db.{DbConfig, TransactorLayer}
import io.iohk.atala.shared.models.{ContextRef, WalletAccessContext, WalletId}
import io.iohk.atala.test.container.PostgresLayer.postgresLayer
import zio.*
import zio.interop.catz.*
import zio.test.*

object JdbcConnectionRepositorySpec extends ZIOSpecDefault {

  private val pgLayer = postgresLayer()
  private val dbConfig = ZLayer.fromZIO(
    for {
      postgres <- ZIO.service[PostgreSQLContainer]
    } yield DbConfig(postgres.username, postgres.password, postgres.jdbcUrl)
  )
  private val transactorLayer = ZLayer.fromZIO {
    for {
      config <- ZIO.service[DbConfig]
      contextRef <- ZIO.service[ThreadLocal[ContextRef[WalletAccessContext]]]
      dispatcher <- Dispatcher[Task].allocated.map { case (dispatcher, _) =>
        given Dispatcher[Task] = dispatcher
        TransactorLayer.hikari[Task](config, contextRef)
      }
    } yield dispatcher
  }.flatten
  private val wacThreadLocal =
    ZLayer.fromZIO(Unsafe.unsafe(implicit unsafe => ContextRef.walletAccessContext.asThreadLocal))
  private val testEnvironmentLayer = pgLayer ++
    (((pgLayer >>> dbConfig) ++ wacThreadLocal) >>> transactorLayer >>> JdbcConnectionRepository.layer) ++
    (pgLayer >>> dbConfig >>> Migrations.layer)

  override def spec: Spec[TestEnvironment with Scope, Any] = {
    (suite("JDBC Connection Repository test suite")(
      ConnectionRepositorySpecSuite.testSuite
    ) @@ TestAspect.sequential @@ TestAspect.before(
      ZIO.serviceWithZIO[Migrations](_.migrate) *>
        ContextRef.walletAccessContext.update(_.copy(Some(WalletAccessContext(WalletId.random))))
    )).provide(testEnvironmentLayer ++ Runtime.enableCurrentFiber)
  }
}
