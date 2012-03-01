package services.signature

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import play.Play
import utils.TestHelpers

class TestXyzmoBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

  "verifyUser" should "return verify match" in {
    val userId: String = "william4"
    val signatureToVerify: String = TestHelpers.getStringFromFile(Play.getFile("test/files/xyzmo_signature7.xml"))
    val response = TestXyzmoBiometricServices.verifyUser(egraphId = 0, userId = userId, signatureDCToVerify = signatureToVerify)
    response.isMatch.get should be(true)
  }

  "verifyUser" should "return verify NO match" in {
    val userId: String = "william4"
    val signatureToVerify: String = TestHelpers.getStringFromFile(Play.getFile("test/files/xyzmo_signature_nomatch.xml"))
    val response = TestXyzmoBiometricServices.verifyUser(egraphId = 0, userId = userId, signatureDCToVerify = signatureToVerify)
    response.isMatch.get should be(false)
  }
}
