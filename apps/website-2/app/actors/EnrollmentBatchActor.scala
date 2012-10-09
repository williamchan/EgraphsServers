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
import play.api.Play.current
import play.api.libs.concurrent.Akka
import akka.actor.Props
import services.config.ConfigFileProxy

object EnrollmentBatchActor {
  val actor = Akka.system.actorOf(Props(AppConfig.instance[EnrollmentBatchActor]))
}

case class EnrollmentBatchActor @Inject()(
  db: DBSession,
  celebrityStore: CelebrityStore,
  enrollmentBatchStore: EnrollmentBatchStore,
  logging: LoggingContext,
  config: ConfigFileProxy
) extends Actor with Logging {
  protected def receive = {
    case ProcessEnrollmentBatchMessage(id: Long) => {
      processEnrollmentBatch(enrollmentBatchId = id)
    }

    case _ =>
  }

  /**
   * Actor method for kicking off an enrollment batch
   * @param enrollmentBatchId
   */
  def processEnrollmentBatch(enrollmentBatchId: Long) {
    config.biometricsStatus match {
      case "offline" =>
      case _ => {
        logging.withTraceableContext("processEnrollmentBatch[" + enrollmentBatchId + "]") {
          db.connected(TransactionSerializable) {
            enrollmentBatchStore.findById(enrollmentBatchId) match {
              case None => throw new Exception("EnrollmentBatchActor could not find EnrollmentBatch " + enrollmentBatchId.toString)
              case Some(enrollmentBatch) if (!enrollmentBatch.isBatchComplete || enrollmentBatch.isSuccessfulEnrollment.isDefined) => {
                throw new Exception("EnrollmentBatchActor did not find EnrollmentBatch in an enrollment state: " + enrollmentBatchId.toString)
              }
              case Some(enrollmentBatch) => attemptEnrollment(enrollmentBatch)
            }
          }
        }
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

    val celebrity = celebrityStore.get(enrollmentBatch.celebrityId)
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
