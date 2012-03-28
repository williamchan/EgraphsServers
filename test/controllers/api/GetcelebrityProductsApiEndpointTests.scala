package controllers.api

import org.junit.Assert._
import play.mvc.Controller
import models.{Product, Celebrity, Account}
import sjson.json.Serializer
import play.test.FunctionalTest
import utils.FunctionalTestUtils.CleanDatabaseAfterEachTest
import org.junit.Test
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}
import utils.{MockControllerMethod, EgraphsUnitTest, TestConstants, FunctionalTestUtils}

class GetCelebrityProductsApiEndpointTests extends EgraphsUnitTest {
  "GetCelebrityProductsApiEndpoint" should "return the serialized products" in {
    val mockFilters = mock[CelebrityAccountRequestFilters]
    
    val account = mock[Account]
    val celeb = mock[Celebrity]
    val product1 = Product(id=100L)
    val product2 = Product(id=200L)

    mockFilters.requireCelebrityAccount(any)(any) answers { case Array(callback: Function2[Account, Celebrity, Any], req) =>
      callback(account, celeb)
    }

    celeb.products() returns (List(product1, product2))
    
    val response = new Controller with GetCelebrityProductsApiEndpoint {
      protected def controllerMethod: ControllerMethod = MockControllerMethod
      protected def celebFilters = mockFilters
    }.getCelebrityProducts
    
    val json = Serializer.SJSON.in[List[Map[String, Any]]](response.toString)
    json.length should be (2)
    json(0)("id") should be (100)
    json(1)("id") should be (200)
  }

  it should "respect the celebrity filter" in {
    val mockFilters = mock[CelebrityAccountRequestFilters]

    mockFilters.requireCelebrityAccount(any)(any) returns "Fail"

    val response = new Controller with GetCelebrityProductsApiEndpoint {
      protected def controllerMethod: ControllerMethod = MockControllerMethod
      protected def celebFilters = mockFilters
    }.getCelebrityProducts

    response should be ("Fail")
  }
}

class GetCelebrityProductsApiEndpointFunctionalTests extends FunctionalTest
  with CleanDatabaseAfterEachTest
{
  import FunctionalTest._

 @Test
 def testGetProductsDoesnt404() {
    FunctionalTestUtils.runScenarios(
      "Will-Chan-is-a-celebrity",
      "Will-has-two-products"
    )

    val response = GET(
      FunctionalTestUtils.willChanRequest, 
      TestConstants.ApiRoot + "/celebrities/me/products"
    )
    
    val json = Serializer.SJSON.in[List[Map[String, Any]]](getContent(response))
    assertStatus(200, response)
    assertEquals(2, json.length)
 }
}