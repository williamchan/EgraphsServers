package services.signature

import org.apache.log4j.Logger


object XyzmoBiometricServices extends SignatureBiometricServicesTrait {

  protected val log: Logger = Logger.getLogger(XyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = true
  protected val domain: String = "ad"
  protected val host: String = "23.21.194.63"
}
