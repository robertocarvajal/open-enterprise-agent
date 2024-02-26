package io.iohk.atala.agent.walletapi.util

import io.iohk.atala.agent.walletapi.crypto.Apollo
import io.iohk.atala.agent.walletapi.crypto.ECKeyPair
import io.iohk.atala.agent.walletapi.model.KeyManagementMode
import io.iohk.atala.agent.walletapi.model.ManagedDIDState
import io.iohk.atala.agent.walletapi.model.WalletSeed
import io.iohk.atala.agent.walletapi.storage.DIDNonSecretStorage
import io.iohk.atala.agent.walletapi.storage.WalletSecretStorage
import io.iohk.atala.castor.core.model.did.EllipticCurve
import io.iohk.atala.castor.core.model.did.PrismDID
import io.iohk.atala.shared.models.WalletAccessContext
import zio.*

class KeyResolver(apollo: Apollo, nonSecretStorage: DIDNonSecretStorage, walletSecretStorage: WalletSecretStorage) {
  def getKey(state: ManagedDIDState, keyId: String): RIO[WalletAccessContext, Option[ECKeyPair]] = {
    val did = state.createOperation.did
    getKey(did, state.keyMode, keyId)
  }

  def getKey(did: PrismDID, keyMode: KeyManagementMode, keyId: String): RIO[WalletAccessContext, Option[ECKeyPair]] = {
    keyMode match {
      case KeyManagementMode.HD => resolveHdKey(did, keyId)
    }
  }

  private def resolveHdKey(did: PrismDID, keyId: String): RIO[WalletAccessContext, Option[ECKeyPair]] = {
    for {
      maybeSeed <- walletSecretStorage.getWalletSeed
      maybeKeyPair <- maybeSeed.fold(ZIO.none) { seed =>
        nonSecretStorage
          .getHdKeyPath(did, keyId)
          .flatMap {
            case None => ZIO.none
            case Some(path) =>
              apollo.secp256k1KeyFactory
                .deriveKeyPair(EllipticCurve.SECP256K1, seed.toByteArray)(path.derivationPath: _*)
                .asSome
          }
      }
    } yield maybeKeyPair
  }
}
