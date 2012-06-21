package services.http

import com.google.inject.{Singleton, Inject}
import java.net.{NetworkInterface, InetAddress}

@Singleton
class HostInfo @Inject()() {
  val macAddress: String = {
    val interface = NetworkInterface.getByInetAddress(localHost)
    val macBytes = interface.getHardwareAddress

    macBytes.map(byte => String.format("%02X", byte.asInstanceOf[AnyRef])).mkString("-")
  }

  val computerName: String = {
    InetAddress.getLocalHost.getHostName
  }

  val userName: String = {
    System.getProperty("user.name")
  }

  private def localHost:InetAddress = {
    InetAddress.getLocalHost
  }
}
