package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{
  willChanRequest, 
  runFreshScenarios, 
  routeName, 
  runWillChanScenariosThroughOrder
}
import play.api.mvc.{AnyContent, AnyContentAsFormUrlEncoded}
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.postEnrollmentSample
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.blobs.Blobs
import services.{Time, AppConfig}
import services.http.HttpCodes
import services.db.{DBSession, TransactionSerializable}
import Blobs.Conversions._
import models.EgraphStore

import models._

class PostEnrollmentSampleApiEndpointTests 
  extends EgraphsUnitTest 
  with ProtectedCelebrityResourceTests 
{
  protected override def routeUnderTest = postEnrollmentSample

  routeName(routeUnderTest) should "accept a well-formed enrollment sample" in new EgraphsTestApplication {
    runFreshScenarios("Will-Chan-is-a-celebrity")

    assertPostEnrollmentSample(
      signatureStr = TestConstants.shortWritingStr,
      voiceStr = TestConstants.voiceStr_8khz(),
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1
    )
  }

  protected override def validRequestBodyAndQueryString = {
    // TODO: Once we're on Play 2.1 then get rid of this necessary indirection to satisfy
    //   invariance of FakeRequest[A], which should be FakeRequest[+A]    
    val formRequest = FakeRequest().withFormUrlEncodedBody(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz()
    )

    val anyContentRequest = formRequest.copy(body=(formRequest.body: AnyContent))

    Some(anyContentRequest)
  }


  it should "complete an enrollment batch when the last required sample is submitted" in new EgraphsTestApplication { 
    runFreshScenarios("Will-Chan-is-a-celebrity")

    val enrollmentBatchId = assertPostEnrollmentSample(signatureStr = TestConstants.shortWritingStr,
      voiceStr = TestConstants.voiceStr_8khz(),
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1)
    for (i <- 1 until EnrollmentBatch.batchSize - 1) {
      assertPostEnrollmentSample(signatureStr = TestConstants.shortWritingStr,
        voiceStr = TestConstants.voiceStr_8khz(),
        isBatchComplete = false,
        numEnrollmentSamplesInBatch = i + 1,
        Some(enrollmentBatchId)
      )
    }
    assertPostEnrollmentSample(signatureStr = TestConstants.shortWritingStr,
      voiceStr = TestConstants.voiceStr_8khz(),
      isBatchComplete = true,
      numEnrollmentSamplesInBatch = 20,
      Some(enrollmentBatchId))
  }

  private def assertPostEnrollmentSample(signatureStr: String,
                                         voiceStr: String,
                                         isBatchComplete: Boolean,
                                         numEnrollmentSamplesInBatch: Int,
                                         enrollmentBatchId: Option[Long] = None): Long = {
    val Some(result) = routeAndCall(
      willChanRequest
      .copy(POST, postEnrollmentSample.url)
      .withFormUrlEncodedBody(
        "signature" -> signatureStr,
        "audio" -> voiceStr
      )
    )
    
    
    status(result) should be (OK)
    val json = Serializer.SJSON.in[Map[String, Any]](contentAsString(result))
    json("id") == null should not be (true)
    json("batch_complete").toString.toBoolean should be (isBatchComplete)

    json("numEnrollmentSamplesInBatch").asInstanceOf[BigDecimal].intValue() should be (numEnrollmentSamplesInBatch)
    json("enrollmentBatchSize").asInstanceOf[BigDecimal].intValue() should be (EnrollmentBatch.batchSize)
    if (enrollmentBatchId.isDefined) json("enrollmentBatchId").asInstanceOf[BigDecimal].longValue() should be (enrollmentBatchId.get)
    else json("enrollmentBatchId") == null should not be (true)

    json("enrollmentBatchId").asInstanceOf[BigDecimal].longValue()
  }
}
