package services.signature

/**
 * IMPORTANT! -- Do not write tests for XyzmoProdBiometricServices or XyzmoBetaBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on Xyzmo.
 * Instead, XyzmoTestBiometricServices exists for automated tests of live Xyzmo server.
 */
trait XyzmoProdBiometricServicesBase extends XyzmoBiometricServicesBase {

  protected val isBasicAuth: Boolean = true
  protected val domain: String = "ad"
  protected val host: String = "107.21.36.146"
}
