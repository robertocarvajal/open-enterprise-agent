package io.iohk.atala.shared.models

import zio.{FiberRef, Unsafe}

import scala.language.implicitConversions

opaque type WalletId = Int

object WalletId {
  def fromInt(id: Int): WalletId = id
  extension (id: WalletId) { def toInt: Int = id }
}

final case class WalletAccessContext(walletId: WalletId)

final case class ContextRef[A](context: Option[A])

object ContextRef {
  val walletAccessContext: FiberRef[ContextRef[WalletAccessContext]] =
    Unsafe.unsafe { implicit unsafe =>
      FiberRef.unsafe.make(ContextRef[WalletAccessContext](None))
    }
}

