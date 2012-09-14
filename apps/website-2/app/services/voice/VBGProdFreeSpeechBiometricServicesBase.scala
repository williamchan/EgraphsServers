package services.voice

import java.net.URL

trait VBGProdFreeSpeechBiometricServicesBase extends VBGBiometricServicesBase {

  override protected val _url: URL = new URL("https://service04.voicebiogroup.com/service/xmlapi")
  override protected val _myClientName: String = "celebrity-fs-en"
  override protected val _myClientKey: String = "7bb22cf2bb6a9d81b8c5c99d4a26c23d"

}