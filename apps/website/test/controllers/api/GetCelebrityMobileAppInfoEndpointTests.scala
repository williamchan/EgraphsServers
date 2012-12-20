package controllers.api

import sjson.json.Serializer
import utils.FunctionalTestUtils.{requestWithCredentials, routeName}
import utils.TestConstants
import play.api.test.Helpers._
import utils.EgraphsUnitTest
import controllers.routes.ApiControllers.getCelebrityMobileAppInfo
import services.Utils
import services.AppConfig
import services.config.ConfigFileProxy
import services.db.DBSession
import services.db.TransactionSerializable
import utils.TestData

class GetCelebrityMobileAppInfoEndpointTests
  extends EgraphsUnitTest 
  with ProtectedCelebrityResourceTests
{
  private def db = AppConfig.instance[DBSession]
  protected override def routeUnderTest = getCelebrityMobileAppInfo

  protected def config = AppConfig.instance[ConfigFileProxy]
  routeName(routeUnderTest) should "return signed request to iPad app archive" in {

    val celebrityAccount = db.connected(TransactionSerializable) {
      val celebrity = TestData.newSavedCelebrity()
      celebrity.account
    }

    val url = controllers.routes.ApiControllers.getCelebrityMobileAppInfo.url
    val Some(result) = routeAndCall(
      requestWithCredentials(celebrityAccount).copy(GET, url)
    )

    status(result) should be (OK)
    val json = Serializer.SJSON.in[Map[String, Map[String, String]]](contentAsString(result))
    val ipadJson = json("ipad")

    val expectedVersion = config.ipadBuildVersion
    ipadJson("version") should be (expectedVersion)
    val ipaUrl = ipadJson("ipaURL")
    ipaUrl.takeWhile(nextChar => nextChar != '?') should be ("https://egraphs-static-resources.s3.amazonaws.com/ipad/Egraphs_" + expectedVersion + ".ipa")
  }

}
