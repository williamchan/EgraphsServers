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
import models.EnrollmentStatus.{Enrolled, NotEnrolled}

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
    var celebrity: Celebrity = null
    var enrollmentBatch: EnrollmentBatch = null
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      celebrity = Celebrity().save()
      enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id, isBatchComplete = true).save()
      celebrity.enrollmentStatus should be(NotEnrolled)
    }

    enrollmentBatchActor !! ProcessEnrollmentBatchMessage(enrollmentBatch.id)
    AppConfig.instance[DBSession].connected(TransactionSerializable) {
      val enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]
      enrollmentBatchStore.findById(enrollmentBatch.id).get.isSuccessfulEnrollment should be(Some(true))
      val celebrityStore = AppConfig.instance[CelebrityStore]
      celebrityStore.findById(celebrity.id).get.enrollmentStatus should be(Enrolled)
    }
  }

}
