package services.voice

import java.net.URL

object VBGDevFreeSpeechBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = new URL("https://service03.voicebiogroup.com/service/xmlapi")
  override protected val _myClientName: String = "celebritydev2"
  override protected val _myClientKey: String = "58b4d89181c2636499cb86fca8e6b911"

}
