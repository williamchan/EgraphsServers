package controllers.api

import play.api.libs.json._
import play.api.test.Helpers._
import play.api.test.FakeRequest
import utils.{ClearsCacheBefore, EgraphsUnitTest}
import utils.FunctionalTestUtils._
import controllers.routes.ApiControllers.getCelebrityProducts
import utils.TestData
import services.AppConfig
import services.db.TransactionSerializable
import models.Product
import models.JsProduct

class GetCelebrityProductsApiEndpointTests 
  extends EgraphsUnitTest 
  with ClearsCacheBefore 
  with ProtectedCelebrityResourceTests
{
  protected def routeUnderTest = getCelebrityProducts
  protected def dbSession = AppConfig.instance[services.db.DBSession]

  routeName(routeUnderTest) should "return the serialized products" in new EgraphsTestApplication {
    // Set up
    val (product, account, celeb) = dbSession.connected(TransactionSerializable) {
      val product = TestData.newSavedProduct()
      val celeb = product.celebrity
      celeb.account.withPassword(TestData.defaultPassword).right.get.save()

      (product, celeb.account, celeb)
    }

    // Issue request
    val request = FakeRequest(GET, routeUnderTest.url).withCredentials(account)

    val Some(result) = route(request)

    // Check result
    status(result) should be (OK)

    val json = Json.parse(contentAsString(result))
    val jsonProducts = json.as[JsArray].value
    (jsonProducts.head.as[JsProduct]).id should be (product.id)
  }
}
