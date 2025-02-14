package io.iohk.atala.pollux.core.service

import io.iohk.atala.event.notification.{Event, EventNotificationService}
import io.iohk.atala.mercury.model.DidId
import io.iohk.atala.mercury.protocol.presentproof.{Presentation, ProofType, ProposePresentation, RequestPresentation}
import io.iohk.atala.pollux.anoncreds.AnoncredPresentation
import io.iohk.atala.pollux.core.model.error.PresentationError
import io.iohk.atala.pollux.core.model.presentation.Options
import io.iohk.atala.pollux.core.model.{DidCommID, PresentationRecord}
import io.iohk.atala.pollux.core.service.serdes.{AnoncredCredentialProofsV1, AnoncredPresentationRequestV1}
import io.iohk.atala.pollux.vc.jwt.{Issuer, PresentationPayload, W3cCredentialPayload}
import io.iohk.atala.shared.models.WalletAccessContext
import zio.{IO, URLayer, ZIO, ZLayer}

import java.time.Instant
import java.util.UUID

class PresentationServiceNotifier(
    svc: PresentationService,
    eventNotificationService: EventNotificationService
) extends PresentationService {

  private val presentationUpdatedEvent = "PresentationUpdated"

  override def createJwtPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      proofTypes: Seq[ProofType],
      options: Option[Options],
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(
      svc.createJwtPresentationRecord(
        pairwiseVerifierDID,
        pairwiseProverDID,
        thid,
        connectionId,
        proofTypes,
        options
      )
    )

  def createAnoncredPresentationRecord(
      pairwiseVerifierDID: DidId,
      pairwiseProverDID: DidId,
      thid: DidCommID,
      connectionId: Option[String],
      presentationRequest: AnoncredPresentationRequestV1
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(
      svc.createAnoncredPresentationRecord(
        pairwiseVerifierDID,
        pairwiseProverDID,
        thid,
        connectionId,
        presentationRequest
      )
    )

  override def markRequestPresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markRequestPresentationSent(recordId))

  override def receiveRequestPresentation(
      connectionId: Option[String],
      request: RequestPresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.receiveRequestPresentation(connectionId, request))

  override def markRequestPresentationRejected(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markRequestPresentationRejected(recordId))

  override def acceptRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: Seq[String]
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.acceptRequestPresentation(recordId, credentialsToUse))

  override def acceptAnoncredRequestPresentation(
      recordId: DidCommID,
      credentialsToUse: AnoncredCredentialProofsV1
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] = notifyOnSuccess(
    svc.acceptAnoncredRequestPresentation(recordId, credentialsToUse)
  )

  override def rejectRequestPresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.rejectRequestPresentation(recordId))

  override def markPresentationGenerated(
      recordId: DidCommID,
      presentation: Presentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationGenerated(recordId, presentation))

  override def markPresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationSent(recordId))

  override def receivePresentation(
      presentation: Presentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.receivePresentation(presentation))

  override def markPresentationVerified(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationVerified(recordId))

  override def markPresentationVerificationFailed(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.markPresentationVerificationFailed(recordId))

  override def verifyAnoncredPresentation(
      presentation: Presentation,
      requestPresentation: RequestPresentation,
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(
      svc.verifyAnoncredPresentation(presentation, requestPresentation, recordId)
    )

  override def acceptPresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.acceptPresentation(recordId))

  override def rejectPresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    notifyOnSuccess(svc.rejectPresentation(recordId))

  private[this] def notifyOnSuccess[R](effect: ZIO[R, PresentationError, PresentationRecord]) =
    for {
      record <- effect
      _ <- notify(record)
    } yield record

  private[this] def notify(record: PresentationRecord) = {
    val result = for {
      walletId <- ZIO.serviceWith[WalletAccessContext](_.walletId)
      producer <- eventNotificationService.producer[PresentationRecord]("Presentation")
      _ <- producer.send(Event(presentationUpdatedEvent, record, walletId))
    } yield ()
    result.catchAll(e => ZIO.logError(s"Notification service error: $e"))
  }

  override def extractIdFromCredential(credential: W3cCredentialPayload): Option[UUID] =
    svc.extractIdFromCredential(credential)

  override def getPresentationRecords(
      ignoreWithZeroRetries: Boolean
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]] =
    svc.getPresentationRecords(ignoreWithZeroRetries)

  override def createJwtPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, PresentationPayload] =
    svc.createJwtPresentationPayloadFromRecord(record, issuer, issuanceDate)

  override def createAnoncredPresentationPayloadFromRecord(
      record: DidCommID,
      issuer: Issuer,
      anoncredCredentialProof: AnoncredCredentialProofsV1,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, AnoncredPresentation] =
    svc.createAnoncredPresentationPayloadFromRecord(record, issuer, anoncredCredentialProof, issuanceDate)

  override def createAnoncredPresentation(
      requestPresentation: RequestPresentation,
      recordId: DidCommID,
      prover: Issuer,
      anoncredCredentialProof: AnoncredCredentialProofsV1,
      issuanceDate: Instant
  ): ZIO[WalletAccessContext, PresentationError, Presentation] =
    svc.createAnoncredPresentation(requestPresentation, recordId, prover, anoncredCredentialProof, issuanceDate)

  override def getPresentationRecordsByStates(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      state: PresentationRecord.ProtocolState*
  ): ZIO[WalletAccessContext, PresentationError, Seq[PresentationRecord]] =
    svc.getPresentationRecordsByStates(ignoreWithZeroRetries, limit, state: _*)

  override def getPresentationRecordsByStatesForAllWallets(
      ignoreWithZeroRetries: Boolean,
      limit: Int,
      state: PresentationRecord.ProtocolState*
  ): IO[PresentationError, Seq[PresentationRecord]] =
    svc.getPresentationRecordsByStatesForAllWallets(ignoreWithZeroRetries, limit, state: _*)

  override def getPresentationRecord(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]] =
    svc.getPresentationRecord(recordId)

  override def getPresentationRecordByThreadId(
      thid: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, Option[PresentationRecord]] =
    svc.getPresentationRecordByThreadId(thid)

  override def receiveProposePresentation(
      request: ProposePresentation
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    svc.receiveProposePresentation((request))

  override def acceptProposePresentation(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    svc.acceptPresentation(recordId)

  override def markProposePresentationSent(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    svc.markProposePresentationSent(recordId)

  override def markPresentationAccepted(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    svc.markPresentationAccepted(recordId)

  override def markPresentationRejected(
      recordId: DidCommID
  ): ZIO[WalletAccessContext, PresentationError, PresentationRecord] =
    svc.markPresentationRejected(recordId)

  override def reportProcessingFailure(
      recordId: DidCommID,
      failReason: Option[_root_.java.lang.String]
  ): ZIO[WalletAccessContext, PresentationError, Unit] = svc.reportProcessingFailure(recordId, failReason)
}

object PresentationServiceNotifier {
  val layer: URLayer[EventNotificationService & PresentationService, PresentationService] =
    ZLayer.fromFunction(PresentationServiceNotifier(_, _))
}
