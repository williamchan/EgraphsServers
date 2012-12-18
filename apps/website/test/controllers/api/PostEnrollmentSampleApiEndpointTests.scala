package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{
  requestWithCredentials,
  routeName
}
import play.api.mvc.{ AnyContent, AnyContentAsFormUrlEncoded }
import utils.TestConstants
import models.EnrollmentBatch
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import play.api.mvc.Result
import play.api.test.FakeRequest
import services.blobs.Blobs
import services.{ Time, AppConfig }
import services.http.HttpCodes
import services.db.{ DBSession, TransactionSerializable }
import Blobs.Conversions._
import models.EgraphStore
import models._
import controllers.WebsiteControllers
import controllers.ApiControllers
import utils.TestData

class PostEnrollmentSampleApiEndpointTests
  extends EgraphsUnitTest
  with ProtectedCelebrityResourceTests
{
  protected override def routeUnderTest = controllers.routes.ApiControllers.postEnrollmentSample
  private def db = AppConfig.instance[DBSession]
  private def enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]

  protected override def validRequestBodyAndQueryString = {
    // TODO: Once we're on Play 2.1 then get rid of this necessary indirection to satisfy
    //   invariance of FakeRequest[A], which should be FakeRequest[+A]    
    val formRequest = FakeRequest().withFormUrlEncodedBody(
      "signature" -> TestConstants.shortWritingStr,
      "audio" -> TestConstants.voiceStr_8khz())

    val anyContentRequest = formRequest.copy(body = (formRequest.body: AnyContent))

    Some(anyContentRequest)
  }

  routeName(routeUnderTest) should "accept a well-formed enrollment sample" in new EgraphsTestApplication {
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {      
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    val result = routeAndCallPostEnrollmentSample(celebrityAccount)

    val enrollmentBatch = db.connected(TransactionSerializable) {
      enrollmentBatchStore.getOpenEnrollmentBatch(celebrity).get // should be one after we post a sample
    }

    assertPostEnrollmentSample(
      result,
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1,
      enrollmentBatch.id)
  }

  it should "complete an enrollment batch when the last requireGd sample is submitted" in new EgraphsTestApplication {
    val (celebrity, celebrityAccount) = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      (celebrity, celebrity.account)
    }

    val result = routeAndCallPostEnrollmentSample(celebrityAccount)

    val enrollmentBatch = db.connected(TransactionSerializable) {
      enrollmentBatchStore.getOpenEnrollmentBatch(celebrity).get // should be one after we post a sample
    }

    // This has a pretty hacky pattern of getting the enrollmentBatchId from the data first run.
    val enrollmentBatchId = assertPostEnrollmentSample(
      result,
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1,
      enrollmentBatch.id)

    for (i <- 1 until EnrollmentBatch.batchSize - 1) {
      val result = routeAndCallPostEnrollmentSample(celebrityAccount)
      assertPostEnrollmentSample(
        result,
        isBatchComplete = false,
        numEnrollmentSamplesInBatch = i + 1,
        enrollmentBatch.id)
    }
    val lastSampleResult = routeAndCallPostEnrollmentSample(celebrityAccount)
    assertPostEnrollmentSample(
      lastSampleResult,
      isBatchComplete = true,
      numEnrollmentSamplesInBatch = 20,
      enrollmentBatch.id)
  }

  private def assertPostEnrollmentSample(result: Result,
    isBatchComplete: Boolean,
    numEnrollmentSamplesInBatch: Int,
    enrollmentBatchId: Long) {

    status(result) should be(OK)
    val json = Serializer.SJSON.in[Map[String, Any]](contentAsString(result))
    json("id") == null should not be (true)
    json("batch_complete").toString.toBoolean should be(isBatchComplete)

    json("numEnrollmentSamplesInBatch").asInstanceOf[BigDecimal].intValue() should be(numEnrollmentSamplesInBatch)
    json("enrollmentBatchSize").asInstanceOf[BigDecimal].intValue() should be(EnrollmentBatch.batchSize)
    json("enrollmentBatchId").asInstanceOf[BigDecimal].longValue() should be(enrollmentBatchId)
  }

  /**
   * Assemble the request and get the result.
   */
  private def routeAndCallPostEnrollmentSample(celebrityAccount: Account,
    signatureStr: String = TestConstants.shortWritingStr,
    voiceStr: String = TestConstants.voiceStr_8khz): Result = {

    val url = controllers.routes.ApiControllers.postEnrollmentSample.url
    val req = requestWithCredentials(celebrityAccount).copy(method = POST, uri = url).withFormUrlEncodedBody(
      "signature" -> signatureStr,
      "audio" -> voiceStr)
    val Some(result) = routeAndCall(req)
    result
  }
}
