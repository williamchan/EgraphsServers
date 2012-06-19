package services.signature

import org.apache.log4j.Logger

object TestlabXyzmoBiometricServices extends XyzmoBiometricServicesBase {

  protected val log: Logger = Logger.getLogger(TestlabXyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = false
  protected val domain: String = "testlab"
  protected val host: String = "testlab.xyzmo.com"
  override protected val _userIdPrefix: String = "testlab"
}