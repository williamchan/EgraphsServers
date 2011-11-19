import java.io.{FileInputStream, File}
import libs.Time
import models.{Account, Celebrity, Customer}
import org.junit.{Assert, After, Test}
import play.mvc.Http.Request
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
    runScenario("Will-Chan-is-a-celebrity")

    // Execute the request
    val response = GET(willChanRequest, apiRoot+"/celebrities/me")

    // Test expectations
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, AnyRef]](getContent(response))

    assertEquals(BigDecimal(1), json("id"))
    assertEquals("Wizzle", json("popularName"))
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
    val response = GET(req, apiRoot+"/celebrities/me")
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
    val response = GET(req, apiRoot+"/celebrities/me/orders?signerActionable=true")
    assertIsOk(response)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))

    val firstOrderJson = json(0)
    val secondOrderJson = json(1)

    // Just check the ids -- the rest is covered by unit tests
    assertEquals(BigDecimal(1L), firstOrderJson("id"))
    assertEquals(BigDecimal(2L), secondOrderJson("id"))
  }

  @Test
  def testSubmitEgraph() {
    runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products",
      "Erem-is-a-customer",
      "Erem-buys-Wills-two-products-twice-each"
    )

    val ordersResponse = GET(willChanRequest, apiRoot+"/celebrities/me/orders?signerActionable=true")
    val ordersList = Serializer.SJSON.in[List[Map[String, Any]]](getContent(ordersResponse))

    val firstOrderMap = ordersList.head
    val orderId = firstOrderMap("id")

    val response = POST(
      willChanRequest,
      apiRoot+"/celebrities/me/orders/"+orderId+"/egraphs",
      APPLICATION_X_WWW_FORM_URLENCODED,
      "signature=theSignature&audio=theAudio"
    )
    assertIsOk(response)

    val json = Serializer.SJSON.in[Map[String, Any]](getContent(response))
    assertEquals(BigDecimal(1), json("id"))
  }

  @Test
  def testSucceedingRequestCommitsTransaction() {
    assertIsOk(GET("/test/request-transaction/without-error"))
    assertEquals("Yep", getContent(GET("/test/request-transaction/is-stored")))
  }

  @Test
  def testFailingTransactionDoesntCommit() {
    try {
      GET("/test/request-transaction/with-error")
    } catch {
      case _ => 1 // This failure was expected
    }

    assertEquals("Nope", getContent(GET("/test/request-transaction/is-stored")))
  }

  @Test
  def testBlobIsAccessibleViaEgraphsLink() {
    runScenario("A-public-image-is-on-the-blobstore")

    val response = GET("/test/files/a/b/derp.jpg")
    assertIsOk(response)

    val actualFile = new File("./test/files/derp.jpg")

    assertEquals(actualFile.length, response.out.toByteArray.length)
  }

  def runScenarios(name: String*) {
    name.foreach { name =>
      runScenario(name)
    }
  }

  def runScenario(name: String) {
    val response = GET("/test/scenarios/"+name)
    if (response.status != 200) {
      throw new IllegalArgumentException("Unknown scenario name "+name)
    }
  }

  def willChanAccount: Account = {
    Account(email="wchan83@gmail.com").withPassword("herp").right.get
  }

  def willChanRequest: Request = {
    val req = newRequest()
    req.user = "wchan83@gmail.com"
    req.password = "herp"

    req
  }
  
  def apiRoot = {
    "/api/1.0"
  }
}