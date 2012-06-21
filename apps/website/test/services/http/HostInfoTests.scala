package services.http

import utils.EgraphsUnitTest

class HostInfoTests extends EgraphsUnitTest {
  def hostInfo = new HostInfo

  "macAddress" should "return the machine's mac address" in {
    hostInfo.macAddress.length should be (17)
  }

  "computerName" should "return a value" in {
    hostInfo.computerName should not be (null)
  }

  "userName" should "return a name" in {
    println(hostInfo.userName)
    hostInfo.userName should not be (null)
    hostInfo.userName should not be ("")
  }
}
