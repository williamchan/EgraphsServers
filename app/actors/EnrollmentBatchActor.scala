package actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import com.google.inject.Inject
import services.logging.{Logging, LoggingContext}
import services.signature.SignatureBiometricsError
import services.voice.VoiceBiometricsError
import models.{CelebrityStore, EnrollmentBatch, EnrollmentBatchStore}
import models.enums.EnrollmentStatus

object EnrollmentBatchActor {
  val actor = actorOf(AppConfig.instance[EnrollmentBatchActor])
}

class EnrollmentBatchActor @Inject()(db: DBSession,
                                     celebrityStore: CelebrityStore,
                                     enrollmentBatchStore: EnrollmentBatchStore,
                                     logging: LoggingContext
                                      ) extends Actor with Logging {
  protected def receive = {
    case ProcessEnrollmentBatchMessage(id: Long) => {
      processEnrollmentBatch(enrollmentBatchId = id)
    }

    case _ =>
  }

  def processEnrollmentBatch(enrollmentBatchId: Long) {
    logging.withTraceableContext("processEnrollmentBatch[" + enrollmentBatchId + "]") {
      db.connected(TransactionSerializable) {
        val enrollmentBatchOption = enrollmentBatchStore.findById(enrollmentBatchId)
        if (enrollmentBatchOption.isEmpty) {
          throw new Exception("EnrollmentBatchActor could not find EnrollmentBatch " + enrollmentBatchId.toString)
        }
        val enrollmentBatch = enrollmentBatchOption.get

        if (!enrollmentBatch.isBatchComplete || enrollmentBatch.isSuccessfulEnrollment.isDefined) {
          throw new Exception("EnrollmentBatchActor did not find EnrollmentBatch in an enrollment state: " + enrollmentBatchId.toString)
        }

        attemptEnrollment(enrollmentBatch)
      }
    }
  }

  private def attemptEnrollment(enrollmentBatch: EnrollmentBatch): Any = {
    val signatureEnrollmentResult: Either[SignatureBiometricsError, Boolean] = enrollmentBatch.enrollSignature
    val isSuccessfulSignatureEnrollment: Boolean = if (signatureEnrollmentResult.isRight) {
      signatureEnrollmentResult.right.get
    } else {
      false
    }

    val voiceEnrollmentResult: Either[VoiceBiometricsError, Boolean] = enrollmentBatch.enrollVoice
    val isSuccessfulVoiceEnrollment: Boolean = if (voiceEnrollmentResult.isRight) {
      voiceEnrollmentResult.right.get
    } else {
      false
    }

    val isSuccessfulEnrollment = isSuccessfulSignatureEnrollment && isSuccessfulVoiceEnrollment
    enrollmentBatch.copy(isSuccessfulEnrollment = Some(isSuccessfulEnrollment)).save()

    val celebrity = celebrityStore.findById(enrollmentBatch.celebrityId).get
    if (isSuccessfulEnrollment) {
      celebrity.withEnrollmentStatus(EnrollmentStatus.Enrolled).save()
    } else {
      celebrity.withEnrollmentStatus(EnrollmentStatus.FailedEnrollment).save()
    }
  }
}

// ====================
// ===== Messages =====
// ====================
sealed trait EnrollmentBatchMessage

case class ProcessEnrollmentBatchMessage(id: Long) extends EnrollmentBatchMessage
