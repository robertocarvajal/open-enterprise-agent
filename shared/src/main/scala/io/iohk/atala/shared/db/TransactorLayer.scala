package io.iohk.atala.shared.db

import cats.effect.Async
import cats.effect.kernel.{Resource, Sync}
import cats.effect.std.Dispatcher
import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import doobie.util.ExecutionContexts
import doobie.util.transactor.Transactor
import io.iohk.atala.shared.models.{ContextRef, WalletAccessContext}
import zio.interop.catz.*
import zio.{Tag, TaskLayer, ZIO, ZLayer}

object TransactorLayer {

  def hikari[A[_]: Async: Dispatcher](config: DbConfig, contextRef: ThreadLocal[ContextRef[WalletAccessContext]])(using
      tag: Tag[Transactor[A]]
  ): TaskLayer[Transactor[A]] = {
    val transactorLayerZio = ZIO
      .attempt {
        // https://github.com/brettwooldridge/HikariCP/wiki/About-Pool-Sizing
        val poolSize = (config.awaitConnectionThreads * 2) + 1
        val hikariConfig = makeHikariConfig(config)
        hikariConfig.setPoolName("DBPool")
        hikariConfig.setLeakDetectionThreshold(300000) // 5 mins
        hikariConfig.setMinimumIdle(poolSize)
        hikariConfig.setMaximumPoolSize(poolSize) // Both Pool size amd Minimum Idle should same and is recommended
        hikariConfig
      }
      .map { hikariConfig =>
        val pool: Resource[A, Transactor[A]] = for {
          // Resource yielding a transactor configured with a bounded connect EC and an unbounded
          // transaction EC. Everything will be closed and shut down cleanly after use.
          ec <- ExecutionContexts.fixedThreadPool[A](config.awaitConnectionThreads) // our connect EC
          ds <- createDataSourceResource(new WalletAwareDataSource(hikariConfig, contextRef))
        } yield Transactor.fromDataSource[A](ds, ec)

        pool.toManaged.toLayer[Transactor[A]]
      }

    ZLayer.fromZIO(transactorLayerZio).flatten
  }

  private def createDataSourceResource[M[_]: Sync](factory: => HikariDataSource): Resource[M, HikariDataSource] = {
    val alloc = Sync[M].delay(factory)
    val free = (ds: HikariDataSource) => Sync[M].delay(ds.close())
    Resource.make(alloc)(free)
  }

  private def makeHikariConfig(config: DbConfig): HikariConfig = {
    val hikariConfig = HikariConfig()

    hikariConfig.setJdbcUrl(config.jdbcUrl)
    hikariConfig.setUsername(config.username)
    hikariConfig.setPassword(config.password)
    hikariConfig.setAutoCommit(false)

    hikariConfig.setDriverClassName("org.postgresql.Driver")
    hikariConfig.addDataSourceProperty("cachePrepStmts", "true")
    hikariConfig.addDataSourceProperty("prepStmtCacheSize", "250")
    hikariConfig.addDataSourceProperty("prepStmtCacheSqlLimit", "2048")

    hikariConfig
  }

}
