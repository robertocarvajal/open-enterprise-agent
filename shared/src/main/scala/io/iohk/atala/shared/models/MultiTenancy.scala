package io.iohk.atala.shared.models

import zio.{FiberRef, Unsafe}
import java.util.UUID
import scala.language.implicitConversions

opaque type WalletId = UUID

object WalletId {
  def fromUUID(uuid: UUID): WalletId = uuid
  def random: WalletId = fromUUID(UUID.randomUUID())

  extension (id: WalletId) { def toUUID: UUID = id }
}

final case class WalletAccessContext(walletId: WalletId)

final case class ContextRef[A](context: Option[A])

object ContextRef {
  val walletAccessContext: FiberRef[ContextRef[WalletAccessContext]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(ContextRef[WalletAccessContext](None))
    }
}
