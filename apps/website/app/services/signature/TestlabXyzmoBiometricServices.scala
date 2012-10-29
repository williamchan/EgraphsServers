package services.signature

import org.slf4j.LoggerFactory
import org.slf4j.Logger

object TestlabXyzmoBiometricServices extends XyzmoBiometricServicesBase {

  override protected val log = LoggerFactory.getLogger(TestlabXyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = false
  protected val domain: String = "testlab"
  protected val host: String = "testlab.xyzmo.com"
  override protected val _userIdPrefix: String = "testlab"
}