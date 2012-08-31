package controllers.api

import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.FunctionalTestUtils.{willChanRequest, runScenario}
import utils.TestConstants
import controllers.website.EgraphsFunctionalTest
import services.Utils

class GetCelebrityMobileAppInfoEndpointTests extends EgraphsFunctionalTest {
  import FunctionalTest._

  @Test
  def testRouteReturnsSignedRequestToAppArchive() {
    runScenario("Will-Chan-is-a-celebrity")
    val expectedVersion = Utils.requiredConfigurationProperty("ipad.buildversion")

    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me/mobileappinfo")
    assertIsOk(response)
    val json = Serializer.SJSON.in[Map[String, Map[String, String]]](getContent(response))
    val ipadJson = json("ipad")
    assertEquals(expectedVersion, ipadJson("version"))
    val ipaUrl = ipadJson("ipaURL")
    assertEquals(true, ipaUrl.startsWith("https://egraphs-static-resources.s3.amazonaws.com/ipad/Egraphs_" + expectedVersion + ".ipa?"))
  }

}
