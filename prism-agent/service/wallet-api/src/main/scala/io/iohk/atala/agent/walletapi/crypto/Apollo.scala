package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.agent.walletapi.util.Prism14CompatUtil.*
import io.iohk.atala.castor.core.model.did.EllipticCurve
import zio.*

import scala.util.Try

final case class ECKeyPair(publicKey: Secp256k1PublicKey, privateKey: Secp256k1PrivateKey)

trait Secp256k1PublicKey {
  final def curve: EllipticCurve = EllipticCurve.SECP256K1 // TODO: deprecate
  def toJavaPublicKey: java.security.interfaces.ECPublicKey
  def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit]
  def encode: Array[Byte]
}

trait Secp256k1PrivateKey {
  final def curve: EllipticCurve = EllipticCurve.SECP256K1 // TODO: deprecate
  def toJavaPrivateKey: java.security.interfaces.ECPrivateKey
  def sign(data: Array[Byte]): Try[Array[Byte]]
  def encode: Array[Byte]
  def computePublicKey: Secp256k1PublicKey
  override final def toString(): String = "<REDACTED>"
}

trait Secp256k1KeyFactory {
  def publicKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[Secp256k1PublicKey]
  def privateKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[Secp256k1PrivateKey]
  def generateKeyPair(curve: EllipticCurve): Task[ECKeyPair]
  def deriveKeyPair(curve: EllipticCurve, seed: Array[Byte])(path: DerivationPath*): Task[ECKeyPair]
  def randomBip32Seed(): Task[(Array[Byte], Seq[String])]
}

enum DerivationPath {
  case Normal(i: Int) extends DerivationPath
  case Hardened(i: Int) extends DerivationPath
}

trait Apollo {
  def secp256k1KeyFactory: Secp256k1KeyFactory
}

object Apollo {
  val prism14Layer: ULayer[Apollo] = ZLayer.succeed(Prism14Apollo)
  val kmpApolloLayer: ULayer[Apollo] = ZLayer.succeed(KmpApollo)
}
