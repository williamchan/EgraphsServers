package services.signature

import org.apache.log4j.Logger

object TestXyzmoBiometricServices extends SignatureBiometricServicesTrait {

  protected val log: Logger = Logger.getLogger(TestXyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = false
  protected val domain: String = "testlab"
  protected val host: String = "testlab.xyzmo.com"

  override def getSignatureDataContainerFromJSON(jsonStr: String): String = {
    // TODO: remove this patch once we have final endpoint to translate JSON signatures to Xyzmo's data format.
    val patchedJsonStr = jsonStr.replace("x", "originalX").replace("y", "originalY").replace("t", "time")
    super.getSignatureDataContainerFromJSON(patchedJsonStr)
  }
}