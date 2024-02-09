package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.prism.apollo.derivation
import io.iohk.atala.prism.apollo.secp256k1.Secp256k1Lib
import io.iohk.atala.prism.apollo.securerandom.SecureRandom
import zio.*

import scala.collection.immutable.ArraySeq
import scala.jdk.CollectionConverters.*
import scala.util.{Try, Failure, Success}

final case class ApolloImplPublicKey(bytes: ArraySeq[Byte]) extends ECPublicKey {

  override def verify(data: Array[Byte], signature: Array[Byte]): Try[Unit] =
    Try(ApolloImpl.secpLib.verify(bytes.toArray, signature, data))
      .flatMap(isValid => if (isValid) Success(()) else Failure(Exception("The signature verification does not match")))

  override def toJavaPublicKey: java.security.interfaces.ECPublicKey =
    // TODO: do not use prism14
    Prism14Apollo.ecKeyFactory
      .publicKeyFromEncoded(curve, encode)
      .get
      .toJavaPublicKey

  override def encode: Array[Byte] = {
    if (bytes.length > 32) ApolloImpl.secpLib.compressPublicKey(bytes.toArray)
    else bytes.toArray
  }

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

}

final case class ApolloImplPrivateKey(bytes: ArraySeq[Byte]) extends ECPrivateKey {

  override def sign(data: Array[Byte]): Try[Array[Byte]] =
    Try(ApolloImpl.secpLib.sign(bytes.toArray, data))

  override def computePublicKey: ECPublicKey =
    ApolloImplPublicKey(ArraySeq.from(ApolloImpl.secpLib.createPublicKey(bytes.toArray, true)))

  override def toJavaPrivateKey: java.security.interfaces.ECPrivateKey =
    // TODO: do not use prism14
    Prism14Apollo.ecKeyFactory
      .privateKeyFromEncoded(curve, encode)
      .get
      .toJavaPrivateKey

  override def encode: Array[Byte] = bytes.toArray

  override def curve: EllipticCurve = EllipticCurve.SECP256K1

}

object ApolloImplFactory extends ECKeyFactory {

  override def publicKeyFromEncoded(curve: EllipticCurve, bytes: Array[Byte]): Try[ECPublicKey] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        Try {
          // TODO: validate bytes?
          ApolloImplPublicKey(ArraySeq.from(bytes))
        }
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
            ApolloImplPublicKey(ArraySeq.from(privateKey.getEncoded())),
            ApolloImplPrivateKey(ArraySeq.from(publicKey.getCompressed())),
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
        Try {
          // TODO: validate bytes?
          ApolloImplPrivateKey(ArraySeq.from(bytes))
        }
      case crv => Failure(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }

  override def generateKeyPair(curve: EllipticCurve): Task[ECKeyPair] = {
    curve match {
      case EllipticCurve.SECP256K1 =>
        ZIO.attempt {
          val privateKey = ApolloImpl.secureRandom.nextBytes(32)
          val publicKey = ApolloImpl.secpLib.createPublicKey(privateKey, true)
          ECKeyPair(
            ApolloImplPublicKey(ArraySeq.from(publicKey)),
            ApolloImplPrivateKey(ArraySeq.from(privateKey)),
          )
        }
      case crv => ZIO.fail(Exception(s"Operation on curve ${crv.name} is not yet supported"))
    }
  }
}

object ApolloImpl extends Apollo {
  val secpLib: Secp256k1Lib = Secp256k1Lib()
  val secureRandom: SecureRandom = SecureRandom()

  override def ecKeyFactory: ECKeyFactory = ApolloImplFactory
}
