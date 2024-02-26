package io.iohk.atala.agent.walletapi.crypto

import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.shared.models.HexString
import zio.*
import zio.test.*
import zio.test.Assertion.*

object ApolloSpec extends ZIOSpecDefault {

  override def spec = {
    val tests = Seq(
      publicKeySpec,
      privateKeySpec,
      ecKeyFactorySpec,
    )

    // KMP Apollo only supports hardened BIP32 derivation
    // https://github.com/input-output-hk/atala-prism-apollo/blob/ce739ddc0477e239c213475b88653229b9781370/apollo/src/commonMain/kotlin/io/iohk/atala/prism/apollo/derivation/HDKey.kt#L164
    //
    // As a consequence, BIP32 test vectors aren't tested against KMP Apollo,
    // but KMP Apollo will be property-test against Prism14Apollo only with hardened index instead.
    suite("Apollo - Prism14 implementation")((tests :+ ecKeyFactoryBip32Spec): _*).provideLayer(Apollo.prism14Layer) +
      suite("Apollo - KMP Apollo implementation")(tests: _*).provideLayer(Apollo.kmpApolloLayer) +
      suite("Apollo - Prism14 and KMP Apollo derive the same BIP32 keys")(compareBip32Spec)
  }

  private val publicKeySpec = suite("ECPublicKey")(
    test("same public key bytes must be equal and have same hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair.publicKey
        pk2 = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, pk1.encode).get
      } yield assert(pk1)(equalTo(pk2)) &&
        assert(pk1 == pk2)(isTrue) &&
        assert(pk1.hashCode())(equalTo(pk2.hashCode()))
    },
    test("different public key bytes must not be equal and have different hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair1.publicKey
        pk2 = keyPair2.publicKey
      } yield assert(pk1)(not(equalTo(pk2))) &&
        assert(pk1 == pk2)(isFalse) &&
        assert(pk1.hashCode())(not(equalTo(pk2.hashCode())))
    },
    test("convert to java PublicKey class") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        javaPk = pk.toJavaPublicKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getW().getAffineX().toString())(
          equalTo("55857268325124588620525700020439091507381445732605907422424441486941792426449")
        ) &&
        assert(javaPk.getW().getAffineY().toString())(
          equalTo("36684214325164537089180371592352190153822062261502257266280631050350493669941")
        )
    },
    test("sign a message and verify using public key") {
      val message = BigInt("42").toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        privateKey = keyPair.privateKey
        publicKey = privateKey.computePublicKey
        signature = privateKey.sign(message).get
      } yield assert(publicKey.verify(message, signature))(isSuccess)
    },
    test("sign a message and verify using different public key should fail") {
      val message = BigInt("42").toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        privateKey = keyPair1.privateKey
        publicKey = keyPair2.publicKey
        signature = privateKey.sign(message).get
      } yield assert(publicKey.verify(message, signature))(isFailure)
    }
  )

  private val privateKeySpec = suite("ECPrivateKey")(
    test("same private key bytes must be equal and have same hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair.privateKey
        pk2 = apollo.secp256k1KeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, pk1.encode).get
      } yield assert(pk1)(equalTo(pk2)) &&
        assert(pk1 == pk2)(isTrue) &&
        assert(pk1.hashCode())(equalTo(pk2.hashCode()))
    },
    test("different private key bytes must not be equal and have different hashCode") {
      for {
        apollo <- ZIO.service[Apollo]
        keyPair1 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        keyPair2 <- apollo.secp256k1KeyFactory.generateKeyPair(EllipticCurve.SECP256K1)
        pk1 = keyPair1.privateKey
        pk2 = keyPair2.privateKey
      } yield assert(pk1)(not(equalTo(pk2))) &&
        assert(pk1 == pk2)(isFalse) &&
        assert(pk1.hashCode())(not(equalTo(pk2.hashCode())))
    },
    test("convert to java PrivateKey class") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk = apollo.secp256k1KeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        javaPk = pk.toJavaPrivateKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getS().toByteArray())(equalTo(bytes))
    },
    test("compute public key from private key") {
      // priv: 0x2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849
      // pub: 0x037b7e17f0524db221af0dd74bd21dec2fc6d0955bbfd43ec7d96ca61dbee2d9d1
      // x: 55857268325124588620525700020439091507381445732605907422424441486941792426449
      // y: 36684214325164537089180371592352190153822062261502257266280631050350493669941
      val compressed = "2789649b57d8f5df144a817f660b494e7a86d465ba86a638a2b525884c5c5849"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        privateKey = apollo.secp256k1KeyFactory.privateKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
        publicKey = privateKey.computePublicKey
        javaPk = publicKey.toJavaPublicKey
      } yield assert(javaPk.getAlgorithm())(equalTo("EC")) &&
        assert(javaPk.getW().getAffineX().toString())(
          equalTo("55857268325124588620525700020439091507381445732605907422424441486941792426449")
        ) &&
        assert(javaPk.getW().getAffineY().toString())(
          equalTo("36684214325164537089180371592352190153822062261502257266280631050350493669941")
        )
    }
  )

  private val ecKeyFactorySpec = suite("ECKeyFactory")(
    test("decode invalid public key should fail") {
      for {
        apollo <- ZIO.service[Apollo]
        decodeResult = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, Array.emptyByteArray)
      } yield assert(decodeResult)(isFailure)
    },
    test("decode valid uncompressed secp256k1 public key successfully") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val uncompressed =
        "0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670"
      val bytes = HexString.fromString(uncompressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode valid compressed secp256k1 public key successfully (even)") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val compressed = "0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode valid compressed secp256k1 public key successfully (odd)") {
      // priv: 0xf7ce236f71334ec2e72c31a7b32d9cbbe32d6ff6dd8fe44cfe54188863898143
      // pub: 0x0355b70ea67cf7341b68c83b67058651478d32be4654f5d31e06c1269529e4f68c
      // x: 38770026255392506965090502006036652798042732817741466608725886725558467098252
      // y: 62085974367854462711068547354621142989016502212980790785921388752460910718337
      val compressed = "0355b70ea67cf7341b68c83b67058651478d32be4654f5d31e06c1269529e4f68c"
      val bytes = HexString.fromString(compressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        _ = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get
      } yield assertCompletes
    },
    test("decode compressed and uncompressed of the same key") {
      // priv: 0xe005dfce415d0ff46485fa37a0f035cf02fedf4b611248eb851a6b563dcf61ed
      // pub: 0x0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68
      // pub: 0x0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670
      // x: 24826623847575292847053498302447868806247480920702231255375760740584438349160
      // y: 56279252673575028438963532839788487911234756883910418647241582529468321625712
      val compressed = "0236e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd68"
      val uncompressed =
        "0436e35f02c325a0cdc3c98968ca5cd51601b0fd8a6e29de4dd73bf0415987bd687c6ced9dbaaa3aca617223c5adaab1109dea0a9a2a75b8fb16361cd19c05f670"
      val bytes = HexString.fromString(compressed).get.toByteArray
      val bytes2 = HexString.fromString(uncompressed).get.toByteArray
      for {
        apollo <- ZIO.service[Apollo]
        pk1 = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes).get.encode
        pk2 = apollo.secp256k1KeyFactory.publicKeyFromEncoded(EllipticCurve.SECP256K1, bytes2).get.encode
      } yield assert(pk1)(equalTo(pk2))
    }
  )

  // https://github.com/bitcoin/bips/blob/master/bip-0032.mediawiki#test-vectors
  private val ecKeyFactoryBip32Spec = {
    def assertHDKey(seedHex: String)(pathStr: String, expectedPrivateKeyHex: String) = {
      val path = pathStr
        .drop(1)
        .split("/")
        .filter(_.nonEmpty)
        .map { s =>
          if (s.endsWith("'")) DerivationPath.Hardened(s.dropRight(1).toInt)
          else DerivationPath.Normal(s.toInt)
        }
        .toSeq
      test(pathStr) {
        val seed = HexString.fromStringUnsafe(seedHex).toByteArray
        for {
          apollo <- ZIO.service[Apollo]
          keyPair <- apollo.secp256k1KeyFactory.deriveKeyPair(EllipticCurve.SECP256K1, seed)(path: _*)
        } yield assert(keyPair.privateKey.encode)(
          equalTo(HexString.fromStringUnsafe(expectedPrivateKeyHex).toByteArray)
        )
      }
    }

    val testVector1 = assertHDKey("000102030405060708090a0b0c0d0e0f")
    val testVector2 = assertHDKey(
      "fffcf9f6f3f0edeae7e4e1dedbd8d5d2cfccc9c6c3c0bdbab7b4b1aeaba8a5a29f9c999693908d8a8784817e7b7875726f6c696663605d5a5754514e4b484542"
    )
    val testVector3 = assertHDKey(
      "4b381541583be4423346c643850da4b320e46a87ae3d2a4e6da11eba819cd4acba45d239319ac14f863b8d5ab5a0d0c64d2e8a1e7d1457df2e5a3c51c73235be"
    )

    suite("ECKeyFactory - BIP32")(
      suite("Test vector 1")(
        testVector1(
          "m",
          "e8f32e723decf4051aefac8e2c93c9c5b214313817cdb01a1494b917c8436b35"
        ),
        testVector1(
          "m/0'",
          "edb2e14f9ee77d26dd93b4ecede8d16ed408ce149b6cd80b0715a2d911a0afea"
        ),
        testVector1(
          "m/0'/1",
          "3c6cb8d0f6a264c91ea8b5030fadaa8e538b020f0a387421a12de9319dc93368"
        ),
        testVector1(
          "m/0'/1/2'",
          "cbce0d719ecf7431d88e6a89fa1483e02e35092af60c042b1df2ff59fa424dca"
        ),
        testVector1(
          "m/0'/1/2'/2",
          "0f479245fb19a38a1954c5c7c0ebab2f9bdfd96a17563ef28a6a4b1a2a764ef4"
        ),
        testVector1(
          "m/0'/1/2'/2/1000000000",
          "471b76e389e528d6de6d816857e012c5455051cad6660850e58372a6c3e6e7c8"
        ),
      ),
      suite("Test vector 2")(
        testVector2(
          "m",
          "4b03d6fc340455b363f51020ad3ecca4f0850280cf436c70c727923f6db46c3e"
        ),
        testVector2(
          "m/0",
          "abe74a98f6c7eabee0428f53798f0ab8aa1bd37873999041703c742f15ac7e1e"
        ),
        testVector2(
          "m/0/2147483647'",
          "877c779ad9687164e9c2f4f0f4ff0340814392330693ce95a58fe18fd52e6e93"
        ),
        testVector2(
          "m/0/2147483647'/1",
          "704addf544a06e5ee4bea37098463c23613da32020d604506da8c0518e1da4b7"
        ),
        testVector2(
          "m/0/2147483647'/1/2147483646'",
          "f1c7c871a54a804afe328b4c83a1c33b8e5ff48f5087273f04efa83b247d6a2d"
        ),
        testVector2(
          "m/0/2147483647'/1/2147483646'/2",
          "bb7d39bdb83ecf58f2fd82b6d918341cbef428661ef01ab97c28a4842125ac23"
        ),
      ),
      suite("Test vector 3")(
        testVector3(
          "m",
          "00ddb80b067e0d4993197fe10f2657a844a384589847602d56f0c629c81aae32"
        ),
        testVector3(
          "m/0'",
          "491f7a2eebc7b57028e0d3faa0acda02e75c33b03c48fb288c41e2ea44e1daef"
        )
      )
    )
  }

  private val compareBip32Spec = suite("BIP32 key derivation comparison")(
    test("all implementations derive the same key for all hardened index") {
      check(Gen.listOfBounded(0, 5)(Gen.int(0, 10000))) { indice =>
        val path = indice.map(DerivationPath.Hardened(_))
        val apollo1 = Prism14Apollo.secp256k1KeyFactory
        val apollo2 = KmpApollo.secp256k1KeyFactory
        for {
          seed <- apollo1.randomBip32Seed().map(_._1)
          keyPair1 <- apollo1.deriveKeyPair(EllipticCurve.SECP256K1, seed)(path: _*)
          keyPair2 <- apollo2.deriveKeyPair(EllipticCurve.SECP256K1, seed)(path: _*)
        } yield assert(keyPair1.privateKey.encode)(equalTo(keyPair2.privateKey.encode))
      }
    }
  )
}
