package controllers.api

import play.api.libs.json._
import play.api.test.FakeRequest
import play.api.test.Helpers._
import utils.FunctionalTestUtils._
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
      FakeRequest(GET, url).withCredentials(celebrityAccount)
    )

    status(result) should be (OK)
    val json = Json.parse(contentAsString(result))
    val ipadJson = (json \ "ipad").as[JsObject]

    val expectedVersion = config.ipadBuildVersion
    (ipadJson \ "version").as[String] should be (expectedVersion)
    val ipaUrl = (ipadJson \ "ipaURL").as[String]
    ipaUrl.takeWhile(nextChar => nextChar != '?') should be ("https://egraphs-static-resources.s3.amazonaws.com/ipad/Egraphs_" + expectedVersion + ".ipa")
  }
}
