package controllers.website

import play.test.FunctionalTest
import java.net.URLDecoder
import play.mvc.Http.Response
import org.junit.After

trait EgraphsFunctionalTest extends FunctionalTest with CleanDatabaseAfterEachTest {

  def getPlayFlashCookie(response: Response): String = {
    URLDecoder.decode(response.cookies.get("PLAY_FLASH").value, "US-ASCII")
  }
}

trait CleanDatabaseAfterEachTest { this: FunctionalTest =>
  @After
  def cleanUpDatabase() {
    FunctionalTest.GET("/test/scenarios/clear")
  }
}
