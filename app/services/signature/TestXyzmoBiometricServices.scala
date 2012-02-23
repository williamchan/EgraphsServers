package services.signature

import org.apache.log4j.Logger

object TestXyzmoBiometricServices extends SignatureBiometricServicesTrait {

  protected val log: Logger = Logger.getLogger(TestXyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = false
  protected val domain: String = "testlab"
  protected val host: String = "testlab.xyzmo.com"
}