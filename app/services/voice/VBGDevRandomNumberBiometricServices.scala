package services.voice

import java.net.URL

object VBGDevRandomNumberBiometricServices extends VBGBiometricServicesBase {

  override protected val _url: URL = new URL("https://service03.voicebiogroup.com/service/xmlapi")
  override protected val _myClientName: String = "celebritydev"
  override protected val _myClientKey: String = "62ed7855e0af30d0af534ce195845c7f"

}
