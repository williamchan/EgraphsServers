package services.http

import com.google.inject.{Singleton, Inject}
import java.net.{NetworkInterface, InetAddress}

/**
 * Provides information about the currently running host.
 */
@Singleton
class HostInfo @Inject()() {
  /**
   * The host's MAC address
   */
  val macAddress: String = {
    val interface = NetworkInterface.getByInetAddress(localHost)
    val macBytes = interface.getHardwareAddress

    macBytes.map(byte => String.format("%02X", byte.asInstanceOf[AnyRef])).mkString("-")
  }

  /**
   * The host's computer name. E.g. "EremMBP" for the author's macbook pro.
   */
  val computerName: String = {
    InetAddress.getLocalHost.getHostName
  }

  /**
   * OS username of the individual running the play server. e.g. "eboto" for the
   * author.
   */
  val userName: String = {
    System.getProperty("user.name")
  }

  //
  // Private members
  //
  private def localHost:InetAddress = {
    InetAddress.getLocalHost
  }
}
