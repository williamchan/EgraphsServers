package services.signature

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import utils.TestConstants
import com.xyzmo.wwww.biometricserver.SDCFromJSONStub.GetSignatureDataContainerFromJSONResponse
import java.io.{File, FileInputStream}
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

class XyzmoBiometricServicesTests extends UnitFlatSpec with ShouldMatchers {

  "getSignatureDataContainerFromJSON" should "translate JSON signature to Xyzmo SignatureDataContainer" in {
    val response: GetSignatureDataContainerFromJSONResponse = XyzmoBiometricServices.getSignatureDataContainerFromJSON(TestConstants.signatureStr)
    response.getGetSignatureDataContainerFromJSONResult should be(getStringFromFile(new File("test/files/xyzmo_signature_nomatch.xml")))
  }

  "verifyUser" should "return verify match" in {
    val userId: String = "william4"
    val signatureToVerify: String = getStringFromFile(new File("test/files/xyzmo_signature7.xml"))
    val response = XyzmoBiometricServices.verifyUser(userId, signatureToVerify)
    response.getOkInfo.getVerifyResult should be(WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch)
  }

  "verifyUser" should "return verify NO match" in {
    val userId: String = "william4"
    val signatureToVerify: String = getStringFromFile(new File("test/files/xyzmo_signature_nomatch.xml"))
    val response = XyzmoBiometricServices.verifyUser(userId, signatureToVerify)
    response.getOkInfo.getVerifyResult should be(WebServiceBiometricPartStub.VerifyResultEnum.VerifyNoMatch)
  }


  private def getStringFromFile(file: File): String = {
    val xmlIn: FileInputStream = new FileInputStream(file)
    val xmlBytes: Array[Byte] = new Array[Byte](xmlIn.available)
    xmlIn.read(xmlBytes)
    new String(xmlBytes)
  }

}