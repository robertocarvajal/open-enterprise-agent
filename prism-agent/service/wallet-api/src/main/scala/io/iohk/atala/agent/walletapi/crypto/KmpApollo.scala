package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.apollo.derivation
import io.iohk.atala.prism.apollo.secp256k1.Secp256k1Lib
import io.iohk.atala.prism.apollo.securerandom.SecureRandom
import io.iohk.atala.prism.apollo.utils.KMMECSecp256k1PrivateKey
import io.iohk.atala.prism.apollo.utils.KMMECSecp256k1PublicKey
import org.bouncycastle.jce.ECNamedCurveTable
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.jce.spec.ECNamedCurveSpec
import zio.*

import java.security.KeyFactory
import java.security.spec.{ECPrivateKeySpec, ECPublicKeySpec}
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Failure, Success}
import io.iohk.atala.shared.models.HexString

final case class KmpApolloPublicKey(publicKey: KMMECSecp256k1PublicKey) extends ECPublicKey {

  override def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit] =
    Try(publicKey.verify(signature, data))
      .flatMap(isValid => if (isValid) Success(()) else Failure(Exception("The signature verification does not match")))

  override def toJavaPublicKey: java.security.interfaces.ECPublicKey = {
    val x = BigInt(publicKey.getCurvePoint().getX())
    val y = BigInt(publicKey.getCurvePoint().getY())
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(EllipticCurve.SECP256K1.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPublicKeySpec = ECPublicKeySpec(java.security.spec.ECPoint(x.bigInteger, y.bigInteger), ecNamedCurveSpec)
    keyFactory.generatePublic(ecPublicKeySpec).asInstanceOf[java.security.interfaces.ECPublicKey]
  }
  override def encode: Array[Byte] = publicKey.getCompressed()

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

  override def hashCode(): Int = HexString.fromByteArray(publicKey.getCompressed()).hashCode()

  override def equals(x: Any): Boolean = {
    x match {
      case KmpApolloPublicKey(otherPK) =>
        HexString.fromByteArray(publicKey.getCompressed()) == HexString.fromByteArray(otherPK.getCompressed())
      case _ => false
    }
  }

}

final case class KmpApolloPrivateKey(privateKey: KMMECSecp256k1PrivateKey) extends ECPrivateKey {

  override def sign(data: Array[Byte]): Try[Array[Byte]] = Try(privateKey.sign(data))

  override def computePublicKey: ECPublicKey = KmpApolloPublicKey(privateKey.getPublicKey())

  override def toJavaPrivateKey: java.security.interfaces.ECPrivateKey = {
    val bytes = privateKey.getEncoded()
    val keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider())
    val ecParameterSpec = ECNamedCurveTable.getParameterSpec(EllipticCurve.SECP256K1.name)
    val ecNamedCurveSpec = ECNamedCurveSpec(
      ecParameterSpec.getName(),
      ecParameterSpec.getCurve(),
      ecParameterSpec.getG(),
      ecParameterSpec.getN()
    )
    val ecPrivateKeySpec = ECPrivateKeySpec(java.math.BigInteger(1, bytes), ecNamedCurveSpec)
    keyFactory.generatePrivate(ecPrivateKeySpec).asInstanceOf[java.security.interfaces.ECPrivateKey]
  }

  override def encode: Array[Byte] = privateKey.getEncoded()

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

  override def hashCode(): Int = HexString.fromByteArray(privateKey.getEncoded()).hashCode()

  override def equals(x: Any): Boolean = {
    x match {
      case KmpApolloPrivateKey(otherPK) =>
        HexString.fromByteArray(privateKey.getEncoded()) == HexString.fromByteArray(otherPK.getEncoded())
      case _ => false
    }
  }

}

object KmpApolloFactory extends ECKeyFactory {

  override def publicKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPublicKey] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try(KMMECSecp256k1PublicKey.Companion.secp256k1FromBytes(bytes))
          .map(pk => KmpApolloPublicKey(pk))
      case crv => Failure(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }

  override def deriveKeyPair(curve: EllipticCurve, seed: Array[Byte])(path: DerivationPath*): Task[ECKeyPair] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        ZIO.attempt {
          val pathStr = path
            .foldLeft(derivation.DerivationPath.empty()) { case (path, p) =>
              p match {
                case DerivationPath.Hardened(i) => path.derive(derivation.DerivationAxis.hardened(i))
                case DerivationPath.Normal(i)   => path.derive(derivation.DerivationAxis.normal(i))
              }
            }
            .toString()
          val hdKey = derivation.HDKey(seed, 0, 0).derive(pathStr)
          val privateKey = hdKey.getKMMSecp256k1PrivateKey()
          val publicKey = privateKey.getPublicKey()

          ECKeyPair(
            KmpApolloPublicKey(publicKey),
            KmpApolloPrivateKey(privateKey)
          )
        }
      case crv => ZIO.fail(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }

  override def randomBip32Seed(): Task[(Array[Byte], Seq[String])] = ZIO.attempt {
    val words = derivation.MnemonicHelper.Companion.createRandomMnemonics()
    val seed = derivation.MnemonicHelper.Companion.createSeed(words, "")
    seed -> words.asScala.toList
  }

  override def privateKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPrivateKey] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try(KMMECSecp256k1PrivateKey.Companion.secp256k1FromByteArray(bytes))
          .map(pk => KmpApolloPrivateKey(pk))
      case crv => Failure(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }

  override def generateKeyPair(curve: EllipticCurve): Task[ECKeyPair] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        ZIO.attempt {
          val randBytes = KmpApollo.secureRandom.nextBytes(32)
          val privateKey = KMMECSecp256k1PrivateKey(randBytes)
          val publicKey = privateKey.getPublicKey
          ECKeyPair(
            KmpApolloPublicKey(publicKey),
            KmpApolloPrivateKey(privateKey),
          )
        }
      case crv => ZIO.fail(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }
}

object KmpApollo extends Apollo {
  val secpLib: Secp256k1Lib = Secp256k1Lib()
  val secureRandom: SecureRandom = SecureRandom()

  override def ecKeyFactory: ECKeyFactory = KmpApolloFactory
}
