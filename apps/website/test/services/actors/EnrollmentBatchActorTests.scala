package services.actors

import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import akka.actor.Actor
import Actor._
import services.db.{DBSession, TransactionSerializable}
import services.AppConfig
import utils.ClearsDatabaseAndValidationBefore
import akka.util.TestKit
import org.scalatest.BeforeAndAfterAll
import actors.{ProcessEnrollmentBatchMessage, EnrollmentBatchActor}
import models._
import enums.EnrollmentStatus

class EnrollmentBatchActorTests extends UnitFlatSpec
with ShouldMatchers
with BeforeAndAfterAll
with ClearsDatabaseAndValidationBefore
with TestKit {

  private val enrollmentBatchActor = actorOf(AppConfig.instance[EnrollmentBatchActor])

  override protected def beforeAll() {
    enrollmentBatchActor.start()
  }

  override protected def afterAll() {
    enrollmentBatchActor.stop()
  }

  it should "respond when an EnrollmentBatch cannot be found by id" in {
    evaluating {
      enrollmentBatchActor !! ProcessEnrollmentBatchMessage(0L)
    } should produce[Exception]
  }

  it should "use EnrollmentBatch to enroll Celebrity" in {
    val (celebrity, enrollmentBatch) = AppConfig.instance[DBSession].connected(TransactionSerializable) {
      val celebrity = Celebrity().save()
      val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, isBatchComplete = true).save()
      (celebrity, enrollmentBatch)
    }
    celebrity.enrollmentStatus should be(EnrollmentStatus.NotEnrolled)

    enrollmentBatchActor !! ProcessEnrollmentBatchMessage(enrollmentBatch.id)
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      AppConfig.instance[EnrollmentBatchStore].get(enrollmentBatch.id).isSuccessfulEnrollment should be(Some(true))
      AppConfig.instance[CelebrityStore].get(celebrity.id).enrollmentStatus should be(EnrollmentStatus.Enrolled)
    }
  }

}
