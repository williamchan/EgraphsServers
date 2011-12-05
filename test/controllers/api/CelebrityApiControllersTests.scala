package controllers.api

import libs.Time
import org.junit.Assert._
import org.junit.Test
import play.test.FunctionalTest
import sjson.json.Serializer
import utils.TestConstants
import utils.FunctionalTestUtils.{CleanDatabaseAfterEachTest, willChanRequest, runScenario, runScenarios}

class CelebrityApiControllersTests extends FunctionalTest with CleanDatabaseAfterEachTest {

  import FunctionalTest._

  @Test
  def testGettingACelebrityReturnsCorrectData() {
    // Set up the scenario
    runScenario("Will-Chan-is-a-celebrity")

    // Execute the request
    val response = GET(willChanRequest, TestConstants.ApiRoot + "/celebrities/me")

    // Test expectations
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, AnyRef]](getContent(response))

    assertNotNull(json("id"))
    assertEquals("Wizzle", json("publicName"))
    assertEquals("William", json("firstName"))
    assertEquals("Chan", json("lastName"))

    // These conversions will fail if they're not Longs
    Time.fromApiFormat(json("created").toString)
    Time.fromApiFormat(json("updated").toString)

    assertEquals(6, json.size)
  }

  @Test
  def testGettingACelebrityWithIncorrectCredentialsFails() {
    // Set up the scenario
    runScenario("Will-Chan-is-a-celebrity")

    // Assemble the request
    val req = newRequest()
    req.user = "wchan83@gmail.com"
    req.password = "wrongwrongwrong"

    // Execute the request
    val response = GET(req, TestConstants.ApiRoot + "/celebrities/me")
    assertEquals(403, response.status)
  }

  @Test
  def testGetCelebrityOrders() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    // Assemble the request
    val req = willChanRequest

    // Execute the request
    val response = GET(req, TestConstants.ApiRoot + "/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))

    val firstOrderJson = json(0)
    val secondOrderJson = json(1)

    // Just check the ids -- the rest is covered by unit tests
    assertEquals(BigDecimal(1L), firstOrderJson("id"))
    assertEquals(BigDecimal(2L), secondOrderJson("id"))
  }
}