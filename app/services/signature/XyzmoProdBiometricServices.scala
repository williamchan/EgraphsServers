package services.signature

import org.apache.log4j.Logger

/**
 * IMPORTANT! -- Do not write tests for XyzmoProdBiometricServices or XyzmoBetaBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on Xyzmo.
 * Instead, XyzmoTestBiometricServices exists for automated tests of live Xyzmo server.
 */
object XyzmoProdBiometricServices extends XyzmoProdBiometricServicesBase {

  protected val log: Logger = Logger.getLogger(XyzmoProdBiometricServices.getClass)
  override protected val _userIdPrefix: String = "prod"
}