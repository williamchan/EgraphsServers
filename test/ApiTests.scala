import org.junit.{Assert, After, Test}
import play.test.FunctionalTest
import sjson.json.Serializer
import Assert._

class ApiTests extends FunctionalTest {
  import FunctionalTest._

  @After
  def cleanEverythingUp() {
    GET("/test/scenarios/clear")
  }

  @Test
  def testGettingACelebrityReturnsCorrectData() {
    // Set up the scenario
    GET("/test/scenarios/Will-Chan-is-a-celebrity")

    // Assemble the request
    val req = newRequest()
    req.user = "wchan83@gmail.com"
    req.password = "herp"

    // Execute the request
    val response = GET(req, apiRoot+"/celebrities/me")

    // Test expectations
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, Any]](getContent(response))

    assertEquals(BigDecimal(1), json("id"))
    assertEquals("Wizzle", json("popularName"))
    assertEquals("William", json("firstName"))
    assertEquals("Chan", json("lastName"))
    assertEquals(4, json.size)
  }

  @Test
  def testGettingACelebrityWithIncorrectCredentialsFails() {
    // Set up the scenario
    GET("/test/scenarios/Will-chan-is-a-celebrity")

    // Assemble the request
    val req = newRequest()
    req.user = "wchan83@gmail.com"
    req.password = "wrongwrongwrong"

    // Execute the request
    val response = GET(req, apiRoot+"/celebrities/me")
    assertEquals(403, response.status)
  }

  def apiRoot = {
    "/api/1.0"
  }
}