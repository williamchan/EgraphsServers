package services.signature

import models._
import models.xyzmo._
import play.api.Play
import utils._
import com.xyzmo.wwww.biometricserver.{WebServiceBiometricPartStub, WebServiceUserAndProfileStub}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

/**
 * IMPORTANT! -- Do not write tests for XyzmoProdBiometricServices or XyzmoBetaBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on Xyzmo.
 * Instead, XyzmoTestBiometricServices exists for automated tests of live Xyzmo server.
 */
@RunWith(classOf[JUnitRunner])
class XyzmoBiometricServicesTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with DBTransactionPerTest
{

  "getSignatureDataContainerFromJSON" should "translate JSON signature to Xyzmo SignatureDataContainer" in new EgraphsTestApplication {
    val signatureStr = "{\"x\": [[67.000000,95.148125,121.414230,151.011597,184.484879,220.291779,255.567886,286.501556,309.481903,323.661255,330.677368,329.015503,314.300873,286.385437,250.558655,217.980347,193.512695,176.263016,165.300140,159.014847,157.115494,160.330505,169.060867,178.425415,184.570480,187.983826,189.809998,191.832565,195.283707,199.417374,204.345871,209.509872,213.854752,217.919907,221.642914,224.634918,227.225143,230.400024,234.414795,239.085846,243.766159,248.264023,253.522644,260.599274,271.696045,288.132111,308.927979,333.386017,359.410614,382.158661,396.454376,402.097565,400.621460,388.776703,364.600494,333.177917,305.941589,287.241943,278.034637,279.602814,290.141541,301.486359,309.847778,315.732635,319.328156,321.800903,323.903931,325.749268,327.481232,329.846252,331.954407,333.949432,335.725739,337.252045,339.370941,342.000000,345.000000]]," +
      " \"y\": [[198.000000,208.518494,226.561005,252.721741,287.361938,326.070160,365.131836,398.816772,423.538239,439.381653,447.187103,443.536896,420.714630,382.285797,336.158752,294.639618,263.300629,242.755737,231.335022,226.617767,228.664505,239.937607,256.833344,271.209839,278.395477,281.191223,282.389954,282.152557,280.267395,277.708832,276.506287,276.149994,275.970337,275.398590,273.784729,271.676941,269.200562,267.133484,265.632111,265.261353,265.670013,267.272583,269.784454,274.343506,282.435089,296.202942,317.578613,348.134338,384.076782,414.948608,433.466217,441.323273,439.651306,423.155914,390.194336,349.909424,316.084259,293.839752,281.174713,281.236938,290.070160,296.798523,301.273621,304.155121,305.342224,304.916199,302.641815,299.708649,296.765503,294.967529,293.286652,292.010834,290.447632,288.095581,285.769043,283.000000,281.000000]]," +
      " \"t\": [[13331445844472,13331448640856,13331448883353,13331449284887,13331449651436,13331450040379,13331450424036,13331450807652,13331451196301,13331451571027,13331452335120,13331452715422,13331453111580,13331453485879,13331453868402,13331454261722,13331454635498,13331455017782,13331455412529,13331455785207,13331456166898,13331456560894,13331456934011,13331457322500,13331457715549,13331458088385,13331458473840,13331458866709,13331459240098,13331459624481,13331460018091,13331460393174,13331460799934,13331461222643,13331461589850,13331461980530,13331462398425,13331462784410,13331463194394,13331463581897,13331463992719,13331464379978,13331464795079,13331465178617,13331465586223,13331465970468,13331466378919,13331466787303,13331467197008,13331467589763,13331467976468,13331468388426,13331468775448,13331469185619,13331469577402,13331469986701,13331470400979,13331470789692,13331471175473,13331471589816,13331471976246,13331472385216,13331472793954,13331473176490,13331473595481,13331473985074,13331474395116,13331474780376,13331475193705,13331475583284,13331475997763,13331476384461,13331476797005,13331477181443,13331477595114,13331477162456,13331477576030]]}"

    val sdc = XyzmoTestBiometricServices.getSignatureDataContainerFromJSON(signatureStr)
    sdc should be(TestHelpers.getStringFromFile(resourceFile("xyzmo_signature_nomatch.xml")))
  }

  "getUserId" should "prepend _userIdPrefix" in new EgraphsTestApplication {
    MockXyzmoBiometricServices.getUserId(1L) should be("mock1")
    TestlabXyzmoBiometricServices.getUserId(celebrityId = 1L) should be("testlab1")
    XyzmoProdBiometricServices.getUserId(celebrityId = 1L) should be("prod1")
    XyzmoBetaBiometricServices.getUserId(celebrityId = 1L) should be("beta1")
    XyzmoTestBiometricServices.getUserId(celebrityId = 1L) should be("test1")
  }

  // todo(wchan): Test enroll and verify instead by using raw JSON files instead of SDCs.
  "XyzmoTestBiometricServices" should "test end-to-end" in new EgraphsTestApplication {
    val signature1 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature1.xml"))
    val signature2 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature2.xml"))
    val signature3 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature3.xml"))
    val signature4 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature4.xml"))
    val signature5 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature5.xml"))
    val signature6 = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature6.xml"))
    val signature_match = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature7.xml"))
    val signature_nomatch = TestHelpers.getStringFromFile(resourceFile("xyzmo_signature_nomatch.xml"))

    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity))
    val order = customer.buy(product).save()

    val enrollmentBatch: EnrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature1, voiceStr = TestConstants.voiceStr())
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature2, voiceStr = TestConstants.voiceStr())
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature3, voiceStr = TestConstants.voiceStr())
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature4, voiceStr = TestConstants.voiceStr())
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature5, voiceStr = TestConstants.voiceStr())
    //    EnrollmentSample(enrollmentBatchId = enrollmentBatch.id).save(signatureStr = signature6, voiceStr = TestConstants.voiceStr())
    //    val enrollmentResult: Either[SignatureBiometricsError, Boolean] = xyzmo.enroll(enrollmentBatch)
    //    enrollmentResult.right.get should be(true)

    // If this test fails intermittently, it could be because we didn't properly clear the Xyzmo service of our User data
    val xyzmoAddUser: XyzmoAddUser = XyzmoTestBiometricServices.addUser(enrollmentBatch = enrollmentBatch)
    xyzmoAddUser.baseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    xyzmoAddUser.error should be(None)
    xyzmoAddUser.errorMsg should be(None)

    val xyzmoAddProfile: XyzmoAddProfile = XyzmoTestBiometricServices.addProfile(enrollmentBatch = enrollmentBatch)
    xyzmoAddProfile.baseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    xyzmoAddProfile.error should be(None)
    xyzmoAddProfile.errorMsg should be(None)
    val profileId = xyzmoAddProfile.xyzmoProfileId.get
    (profileId.length() > 0) should be(true)

    val xyzmoEnrollUser: XyzmoEnrollDynamicProfile = XyzmoTestBiometricServices.enrollUser(enrollmentBatch = enrollmentBatch, List(signature1, signature2, signature3, signature4, signature5, signature6))
    xyzmoEnrollUser.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoEnrollUser.error should be(None)
    xyzmoEnrollUser.errorMsg should be(None)
    xyzmoEnrollUser.enrollResult.get should be(WebServiceBiometricPartStub.EnrollResultEnum.EnrollCompleted.getValue)
    xyzmoEnrollUser.nrEnrolled.get should be(6)
    (xyzmoEnrollUser.xyzmoProfileId.get) should be(profileId)
    xyzmoEnrollUser.rejectedSignaturesSummary should be(None)

    val egraphVerifyTrue: Egraph = Egraph(orderId = order.id).save()
    val xyzmoVerifyUser_Match: XyzmoVerifyUser = XyzmoTestBiometricServices.verifyUser(egraph = egraphVerifyTrue, signature_match)
    xyzmoVerifyUser_Match.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoVerifyUser_Match.error should be(None)
    xyzmoVerifyUser_Match.errorMsg should be(None)
    xyzmoVerifyUser_Match.isMatch.get should be(true)
    (xyzmoVerifyUser_Match.score.get > 90) should be(true)

    val egraphVerifyFalse: Egraph = Egraph(orderId = order.id).save()
    val xyzmoVerifyUser_NoMatch: XyzmoVerifyUser = XyzmoTestBiometricServices.verifyUser(egraph = egraphVerifyFalse, signature_nomatch)
    xyzmoVerifyUser_NoMatch.baseResult should be(WebServiceBiometricPartStub.BaseResultEnum.ok.getValue)
    xyzmoVerifyUser_NoMatch.error should be(None)
    xyzmoVerifyUser_NoMatch.errorMsg should be(None)
    xyzmoVerifyUser_NoMatch.isMatch.get should be(false)
    xyzmoVerifyUser_NoMatch.score.get should be(0)

    val xyzmoDeleteUser: XyzmoDeleteUser = XyzmoTestBiometricServices.deleteUser(enrollmentBatch = enrollmentBatch)
    xyzmoDeleteUser.baseResult should be(WebServiceUserAndProfileStub.BaseResultEnum.ok.getValue)
    xyzmoDeleteUser.error should be(None)
    xyzmoDeleteUser.errorMsg should be(None)
  }
}
