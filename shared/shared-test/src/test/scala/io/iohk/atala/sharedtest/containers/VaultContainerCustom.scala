package io.iohk.atala.sharedtest.containers

import com.dimafeng.testcontainers.{SingleContainer, VaultContainer}
import org.testcontainers.vault.{VaultContainer => JavaVaultContainer}
import org.testcontainers.utility.DockerImageName

/** See PostgreSQLContainerCustom for explanation */
class VaultContainerCustom(
    dockerImageNameOverride: DockerImageName,
    vaultToken: Option[String] = None,
    secrets: Option[VaultContainer.Secrets] = None,
    isOnGithubRunner: Boolean = false
) extends SingleContainer[JavaVaultContainer[_]] {

  private val vaultContainer: JavaVaultContainer[_] = new JavaVaultContainer(dockerImageNameOverride) {
    override def getHost: String = {
      if (isOnGithubRunner) super.getContainerId().take(12)
      else super.getHost()
    }
    override def getMappedPort(originalPort: Int): Integer = {
      if (isOnGithubRunner) 8200
      else super.getMappedPort(originalPort)
    }
  }

  if (vaultToken.isDefined) vaultContainer.withVaultToken(vaultToken.get)
  secrets.foreach { x =>
    vaultContainer.withSecretInVault(x.path, x.firstSecret, x.secrets: _*)
  }

  override val container: JavaVaultContainer[_] = vaultContainer
}
