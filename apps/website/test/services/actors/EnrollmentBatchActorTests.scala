package services.actors

import scala.concurrent._
import scala.concurrent.duration._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import akka.actor._
import akka.actor.Actor._
import akka.pattern._
import akka.util.Timeout
import play.api.Application
import play.api.libs.concurrent.Akka
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import utils.{TestData, ClearsCacheBefore, EgraphsUnitTest}
import actors.{ProcessEnrollmentBatchMessage, EnrollmentBatchActor}
import models._
import enums.EnrollmentStatus
import utils.TestHelpers.withActorUnderTest

@RunWith(classOf[JUnitRunner])
class EnrollmentBatchActorTests extends EgraphsUnitTest
  with ClearsCacheBefore
{
  implicit val timeout: Timeout = 1 second 

  it should "use EnrollmentBatch to enroll Celebrity" in new EgraphsTestApplication {
    withActorUnderTest(AppConfig.instance[EnrollmentBatchActor]) { enrollmentBatchActor =>
      val (celebrity, enrollmentBatch) = AppConfig.instance[DBSession].connected(TransactionSerializable) {
        val celebrity = TestData.newSavedCelebrity().withEnrollmentStatus(EnrollmentStatus.NotEnrolled).save()
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

