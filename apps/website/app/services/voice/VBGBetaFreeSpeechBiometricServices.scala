package services.voice

import java.net.URL


/**
 * IMPORTANT! -- Do not write tests for VBGProdFreeSpeechBiometricServices or VBGBetaFreeSpeechBiometricServices.
 * They will clobber live data because we use Celebrity IDs as userIds on VBG.
 * Instead, VBGTestFreeSpeechBiometricServices exists for automated tests of celebrity-fs-en account.
 */
object VBGBetaFreeSpeechBiometricServices extends VBGProdFreeSpeechBiometricServicesBase {

  override protected val _userIdPrefix: String = "beta"

  // testing service04 before making full switchover from service02
  override protected val _url: URL = new URL("https://service04.voicebiogroup.com/service/xmlapi")
}
