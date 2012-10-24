package services.actors

import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import utils.{TestData, ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import actors.{ProcessEnrollmentBatchMessage, EnrollmentBatchActor}
import models._
import enums.EnrollmentStatus
import play.api.Application
import play.api.libs.concurrent.Akka
import akka.actor.Props
import akka.actor.ActorRef
import akka.pattern._
import akka.util.Timeout
import akka.util.duration._
import utils.TestHelpers.withActorUnderTest
import akka.dispatch.Await
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class EnrollmentBatchActorTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
{
  implicit val timeout: Timeout = 1 second 

  it should "use EnrollmentBatch to enroll Celebrity" in new EgraphsTestApplication {
    withActorUnderTest(AppConfig.instance[EnrollmentBatchActor]) { enrollmentBatchActor =>
      val (celebrity, enrollmentBatch) = AppConfig.instance[DBSession].connected(TransactionSerializable) {
        val celebrity = TestData.newSavedCelebrity()
        val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, isBatchComplete = true).save()
        (celebrity, enrollmentBatch)
      }
      celebrity.enrollmentStatus should be(EnrollmentStatus.NotEnrolled)
  
      val future = enrollmentBatchActor ask ProcessEnrollmentBatchMessage(enrollmentBatch.id) // We don't care about the actual response, just that it finished.
      Await.result(future, 1 second)

      AppConfig.instance[DBSession].connected(TransactionSerializable) {
        AppConfig.instance[EnrollmentBatchStore].get(enrollmentBatch.id).isSuccessfulEnrollment should be(Some(true))
        AppConfig.instance[CelebrityStore].get(celebrity.id).enrollmentStatus should be(EnrollmentStatus.Enrolled)
      }
    }
  }
}

