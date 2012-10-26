package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{willChanRequest, runFreshScenarios, routeName}
import utils.TestConstants
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.getCelebrityMobileAppInfo
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy

class GetCelebrityMobileAppInfoEndpointTests
  extends EgraphsUnitTest 
  with ProtectedCelebrityResourceTests
{
  protected override def routeUnderTest = getCelebrityMobileAppInfo

  protected def config = AppConfig.instance[ConfigFileProxy]
  routeName(routeUnderTest) should "return signed request to iPad app archive" in {
    
    runFreshScenarios("Will-Chan-is-a-celebrity")
    
    val expectedVersion = config.ipadBuildVersion
    val Some(result) = routeAndCall(willChanRequest.copy(method=GET, uri=getCelebrityMobileAppInfo.url))
    
    status(result) should be (OK)
    val json = Serializer.SJSON.in[Map[String, Map[String, String]]](contentAsString(result))
    val ipadJson = json("ipad")
    
    ipadJson("version") should be (expectedVersion)
    val ipaUrl = ipadJson("ipaURL")
    ipaUrl.takeWhile(nextChar => nextChar != '?') should be ("https://egraphs-static-resources.s3.amazonaws.com/ipad/Egraphs_" + expectedVersion + ".ipa")
  }

}
