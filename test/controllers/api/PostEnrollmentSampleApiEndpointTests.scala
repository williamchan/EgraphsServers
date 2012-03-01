package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenarios}
import models._
import utils.TestConstants

class PostEnrollmentSampleApiEndpointTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  @Test
  def testPostEnrollmentSample() {
    runScenarios(
      "Will-Chan-is-a-celebrity"
    )

    assertPostEnrollmentSample(signatureStr = TestConstants.signatureStr,
      voiceStr = TestConstants.voiceStrPercentEncoded(),
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1)
  }

  @Test
  def testPostEnrollmentSampleCompletingBatch() {
    runScenarios(
      "Will-Chan-is-a-celebrity"
    )

    val enrollmentBatchId = assertPostEnrollmentSample(signatureStr = TestConstants.signatureStr,
      voiceStr = TestConstants.voiceStrPercentEncoded(),
      isBatchComplete = false,
      numEnrollmentSamplesInBatch = 1)
    for (i <- 1 until EnrollmentBatch.batchSize - 1) {
      assertPostEnrollmentSample(signatureStr = TestConstants.signatureStr,
        voiceStr = TestConstants.voiceStrPercentEncoded(),
        isBatchComplete = false,
        numEnrollmentSamplesInBatch = i + 1,
        Some(enrollmentBatchId)
      )
    }
    assertPostEnrollmentSample(signatureStr = TestConstants.signatureStr,
      voiceStr = TestConstants.voiceStrPercentEncoded(),
      isBatchComplete = true,
      numEnrollmentSamplesInBatch = 20,
      Some(enrollmentBatchId))
  }

  private def assertPostEnrollmentSample(signatureStr: String,
                                         voiceStr: String,
                                         isBatchComplete: Boolean,
                                         numEnrollmentSamplesInBatch: Int,
                                         enrollmentBatchId: Option[Long] = None): Long = {
    val response = POST(
      willChanRequest,
      TestConstants.ApiRoot + "/celebrities/me/enrollmentsamples",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=" + signatureStr + "&audio=" + voiceStr + "&skipBiometrics=1"
    )
    assertIsOk(response)
    val json = Serializer.SJSON.in[Map[String, Any]](getContent(response))
    assertNotNull(json("id"))
    assertEquals(isBatchComplete, json("batch_complete"))

    assertEquals(numEnrollmentSamplesInBatch, json("numEnrollmentSamplesInBatch").asInstanceOf[BigDecimal].intValue())
    assertEquals(EnrollmentBatch.batchSize, json("enrollmentBatchSize").asInstanceOf[BigDecimal].intValue())
    if (enrollmentBatchId.isDefined) assertEquals(enrollmentBatchId.get, json("enrollmentBatchId").asInstanceOf[BigDecimal].longValue())
    else assertNotNull(json("enrollmentBatchId"))

    json("enrollmentBatchId").asInstanceOf[BigDecimal].longValue()
  }

}
