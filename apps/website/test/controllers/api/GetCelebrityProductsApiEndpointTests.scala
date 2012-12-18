package controllers.api

import sjson.json.Serializer
import utils.{ClearsCacheBefore, EgraphsUnitTest, TestConstants, FunctionalTestUtils}
import utils.FunctionalTestUtils.{requestWithCredentials, routeName}
import controllers.routes.ApiControllers.getCelebrityProducts
import utils.TestData
import services.AppConfig
import play.api.test.Helpers._
import services.db.TransactionSerializable

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
      val account = celeb.account.withPassword(TestData.defaultPassword).right.get.save()

      (product, celeb.account, celeb)
    }

    // Issue request
    val request = requestWithCredentials(account.email, TestData.defaultPassword)
      .copy(method=GET, uri=routeUnderTest.url)

    val Some(result) = routeAndCall(request)

    // Check result
    status(result) should be (OK)

    val json = Serializer.SJSON.in[List[Map[String, Any]]](contentAsString(result))
    json(0)("id") should be (product.id)
  }
}
