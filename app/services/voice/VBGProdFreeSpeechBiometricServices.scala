package services.voice

import java.net.URL

/**
 * IMPORTANT! -- Do not write tests for VBGProdFreeSpeechBiometricServices that will clobber actual accounts on VBG.
 * We use Celebrity IDs as userIds on VBG.
 */
object VBGProdFreeSpeechBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = new URL("https://service02.voicebiogroup.com/service/xmlapi")
  override protected val _myClientName: String = "celebrity-fs-en"
  override protected val _myClientKey: String = "7bb22cf2bb6a9d81b8c5c99d4a26c23d"

}
