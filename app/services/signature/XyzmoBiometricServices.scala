package services.signature

import org.apache.log4j.Logger

/**
 * IMPORTANT! -- Do not write tests for this object that will clobber actual accounts on VBG. We use Celebrity IDs as
 * userIds on VBG.
 */
object XyzmoBiometricServices extends XyzmoBiometricServicesBase {

  protected val log: Logger = Logger.getLogger(XyzmoBiometricServices.getClass)
  protected val isBasicAuth: Boolean = true
  protected val domain: String = "ad"
  protected val host: String = "23.21.194.63"
}
