package services.cache

import utils.{TestData, EgraphsUnitTest}
import services.AppConfig
import services.http.{HostInfo, DeploymentTarget}

class ApplicationCacheTests extends EgraphsUnitTest {
  import DeploymentTarget._

  "hostId" should "be the application id when in staging, demo, live" in {
    for (playId <- List(Staging, Demo, Live).map(_.name)) {
      makeApplicationCache.copy(playId=playId).hostId should be (playId)
    }
  }

  "hostId" should "include the application id and some identifying info during test" in {
    val hostInfo = AppConfig.instance[HostInfo]
    val hostId = makeApplicationCache.hostId

    hostId.contains(Test.name) should be (true)
    hostId.contains(hostInfo.macAddress) should be (true)
    hostId.contains(hostInfo.userName) should be (true)
    hostId.contains(hostInfo.computerName) should be (true)
  }

  "get and set" should "work well together" in {
    val key = TestData.makeTestCacheKey

    val cache = makeApplicationCache
    cache.set(key, "herp", Some("1s"))
    cache.cacheFactory().get(cache.fullKey(key)) should be (Some("herp"))
    cache.get(key) should be (Some("herp"))
    cache.delete(key)
  }

  private def makeApplicationCache = {
    AppConfig.instance[ApplicationCache]
  }
}
