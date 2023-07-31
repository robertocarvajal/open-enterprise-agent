package io.iohk.atala.shared.db

import com.zaxxer.hikari.{HikariConfig, HikariDataSource}
import io.iohk.atala.shared.models.{ContextRef, WalletAccessContext}

import java.sql.Connection

class WalletAwareDataSource(
    hikariConfig: HikariConfig,
    contextRef: ThreadLocal[ContextRef[WalletAccessContext]]
) extends HikariDataSource(hikariConfig) {
  override def getConnection: Connection = {
    val conn = super.getConnection
    contextRef.get().context match
      case Some(wac) =>
        val stmt = conn.createStatement()
        stmt.execute(s"SET app.walletId = '${wac.walletId}'")
        conn
      case None => conn
  }
}
