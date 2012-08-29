package services.http

import play.test.FunctionalTest
import models._
import enums.PublishedStatus
import play.mvc.Http.Request
import play.mvc.results.{Forbidden, NotFound, Ok}
import utils.{ClearsCacheAndBlobsAndValidationBefore, EgraphsUnitTest}
import java.util.Properties
import play.mvc.Scope.Session
import services.Utils

class CelebrityAccountRequestFiltersTests extends EgraphsUnitTest with ClearsCacheAndBlobsAndValidationBefore {

  private def niceAccountFilter(account: Account=mock[Account]) = {
    // Returns an AccountRequestFilter that always yields a particular account
    val accountFilters = mock[AccountRequestFilters]

    accountFilters.requireAuthenticatedAccount(any)(any) answers { case Array(continue: Function1[Account, Any], _) =>
      continue(account)
    }

    accountFilters
  }

  private def instance(celebStore: CelebrityStore=null,
                       accountFilters: AccountRequestFilters=null,
                       productFilters:ProductQueryFilters=null,
                       administratorStore: AdministratorStore=null,
                       playConfig: Properties=Utils.properties("admin.tools.enabled" -> "restricted")) = {
    new CelebrityAccountRequestFilters(celebStore, accountFilters, productFilters, administratorStore, playConfig)
  }
  
  private def requestWithCelebrityId(celebrityId: String) = {
    val request = FunctionalTest.newRequest()

    request.params.put("celebrityId", celebrityId)

    request
  }

  private def setupCelebrityMocks(name: String = "Shaq", publishedStatus: PublishedStatus.EnumVal) : (Celebrity, CelebrityStore) = {
    val celebStore = mock[CelebrityStore]
    val celebrity = mock[Celebrity]
    celebStore.findByUrlSlug(name) returns (Some(celebrity))
    celebrity.publishedStatus returns publishedStatus
    celebrity.publicName returns name
    (celebrity, celebStore)
  }

  "requireCelebrityAccount" should "return 403-Forbidden if celebrityId is empty, null, or not the string 'me'" in {
    // Set up
    val celebrityIds = List("", "Celebrity-That-Isnt-'me'", null)

    for (celebrityId <- celebrityIds) {
      implicit val request = requestWithCelebrityId(celebrityId)
      implicit val session = mock[play.mvc.Scope.Session]

      val blockThatShouldntExecute = mock[Function2[Account, Celebrity, Any]]

      // Execute test
      val result = instance(accountFilters=niceAccountFilter())
        .requireCelebrityAccount(blockThatShouldntExecute)

      // Check expectations
      result.isInstanceOf[Forbidden] should be (true)
      there was no(blockThatShouldntExecute).apply(any, any)
    }
  }

  it should "Return a server error if the request credentials specify an account that has no celebrity face" in {
    // Set up
    implicit val request = requestWithCelebrityId("me")
    implicit val session = mock[play.mvc.Scope.Session]

    val blockThatShouldntExecute = mock[Function2[Account, Celebrity, Any]]
    val account = mock[Account]

    account.celebrityId returns (None)

    // Run test
    val result = instance(accountFilters=niceAccountFilter(account))
      .requireCelebrityAccount(blockThatShouldntExecute)

    // Check expectations
    result.isInstanceOf[Forbidden] should be (true)
    there was no(blockThatShouldntExecute).apply(any, any)
  }

  it should "call back with the correct Account and Celebrity if celebrityId is 'me' and an Account with Celebrity face exists" in {
    // Set up
    implicit val request = requestWithCelebrityId("me")
    implicit val session = mock[play.mvc.Scope.Session]

    val continuationBlock = mock[Function2[Account, Celebrity, Any]]
    continuationBlock.apply(any,any) returns (new Ok)

    val account = mock[Account]
    account.celebrityId returns (Some(1L))

    val celebStore = mock[CelebrityStore]
    val celebrity = mock[Celebrity]
    celebStore.get(1L) returns (celebrity)

    // Run test
    val result = instance(celebStore=celebStore, accountFilters=niceAccountFilter(account))
      .requireCelebrityAccount(continuationBlock)

    // Check expectations
    result.isInstanceOf[Ok] should be (true)
    there was one(continuationBlock).apply(account, celebrity)
  }

  "requireCelebrityUrlSlug" should "throw up if celebrityUrlSlug is not provided" in {
    for (value <- List("", null)) {
      implicit val (request, session) = newRequestAndMockSession
      request.params.put("celebrityUrlSlug", value)
      
      val blockToCall = mock[Function1[Celebrity, Any]]

      evaluating { instance().requireCelebrityUrlSlug(blockToCall) } should produce [IllegalStateException]
      there was no (blockToCall).apply(any)
    }
  }

  "requireCelebrityUrlSlug" should "find celebritiy with the provided url slug then continue via callback" in {
    implicit val (request, session) = newRequestAndMockSession
    request.params.put("celebrityUrlSlug", "Shaq")
    val (celebrity, celebStore) = setupCelebrityMocks(publishedStatus = PublishedStatus.Published)

    val result = instance(celebStore=celebStore).requireCelebrityUrlSlug { celebrity =>
      "found celebrity"
    }
    result should be ("found celebrity")
  }

  "requireCelebrityUrlSlug" should "not return celebrity that is unpublished" in {
    implicit val (request, session) = newRequestAndMockSession
    request.params.put("celebrityUrlSlug", "Shaq")
    val (celebrity, celebStore) = setupCelebrityMocks(publishedStatus = PublishedStatus.Unpublished)

    val result = instance(celebStore=celebStore).requireCelebrityUrlSlug { celebrity => "ok"  }
    result.isInstanceOf[NotFound] should be (true)
  }

  "requireCelebrityUrlSlug" should "return unpublished celebrity if user is admin and full admin tools are enabled" in {
    implicit val (request, session) = newRequestAndMockSession
    request.params.put("celebrityUrlSlug", "Shaq")
    val (celebrity, celebStore) = setupCelebrityMocks(publishedStatus = PublishedStatus.Unpublished)
    val adminStore = mock[AdministratorStore]
    adminStore.isAdmin(None) returns true

    val result = instance(celebStore=celebStore, administratorStore = adminStore, playConfig = Utils.properties("admin.tools.enabled" -> "full"))
      .requireCelebrityUrlSlug { celebrity => "ok"  }
    result should be("ok")
  }

  "requireCelebrityProductUrl" should "throw up if productUrlSlug is not provided" in {
    for (value <- List("", null)) {
      implicit val (request, session) = newRequestAndMockSession
      request.params.put("productUrlSlug", value)
  
      val blockToCall = mock[Function1[Product, Any]]

      evaluating { instance().requireCelebrityProductUrl(Celebrity())(blockToCall) } should produce [IllegalStateException]
      there was no (blockToCall).apply(any)
    }
  }

  /*it should "return 404-Not Found if the celebrity didn't have a product with the specified URL" in {
    implicit val (request, session) = newRequestAndMockSession
    request.params.put("productUrlSlug", "Finals")

    val mockQueryFilter = mock[FilterOneTable[Product]]
    
    val productFilters = mock[ProductQueryFilters]
    productFilters.byUrlSlug("Finals") returns (mockQueryFilter)
    
    val celebrity = mock[Celebrity]
    celebrity.publicName returns (Some("Shaq"))
    celebrity.products(mockQueryFilter).toList returns (List.empty[Product])

    val callback = mock[Function1[Product, Any]]
    val result = instance(productFilters=productFilters)
      .requireCelebrityProductUrl(celebrity)(callback)

    result.isInstanceOf[NotFound] should be (true)
    there was no (callback).apply(any)
  }

  it should "execute the provided callback if the celebrity had a matching product" in {
    implicit val (request, session) = newRequestAndMockSession
    request.params.put("productUrlSlug", "Finals")

    val mockQueryFilter = mock[FilterOneTable[Product]]

    val productFilters = mock[ProductQueryFilters]
    productFilters.byUrlSlug("Finals") returns (mockQueryFilter)

    val product = mock[Product]
    val celebrity = mock[Celebrity]
    celebrity.products(mockQueryFilter).toList returns (List(product))

    val callback = mock[Function1[Product, Any]]
    callback.apply(any) returns (new Ok)

    val result = instance(productFilters=productFilters)
      .requireCelebrityProductUrl(celebrity)(callback)

    result.isInstanceOf[Ok] should be (true)
    there was one (callback).apply(product)
  }*/

  "requireCelebrityAndProductUrlSlugs" should "delegate to requireCelebrityUrlSlug and requireCelebrityProductUrl" in {
    object FailsCelebrityUrlSlug extends CelebrityAccountRequestFilters(null, null, null, null, null) {
      override def requireCelebrityUrlSlug(continue: (Celebrity) => Any)(implicit request: Request, session: Session) = {
        "Stopped at requireCelebrityUrlSlug"
      }
    }

    implicit val (request, session) = newRequestAndMockSession
    val result = FailsCelebrityUrlSlug.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      "Failed"
    }

    result should be ("Stopped at requireCelebrityUrlSlug")
  }

  it should "secondarily delegate to requireCelebrityProductUrl" in {
    val celebrity = mock[Celebrity]

    object FailsProductUrl extends CelebrityAccountRequestFilters(null, null, null, null, null) {
      override def requireCelebrityUrlSlug(continue: (Celebrity) => Any)(implicit request: Request, session: Session) = {
        continue(celebrity)
      }

      override def requireCelebrityProductUrl(celebrity: Celebrity)(continue: (Product) => Any)(implicit request: Request, session: Session) = {
        "Stopped at requireCelebrityProductUrl"
      }
    }

    implicit val (request, session) = newRequestAndMockSession
    val result = FailsProductUrl.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      "Failed"
    }

    result should be ("Stopped at requireCelebrityProductUrl")
  }

  it should "execute the callback with the celebrity and product provided by its two delegated filters" in {
    val (celebrity, product) = (mock[Celebrity], mock[Product])
    
    object PassesDelegatedFilters extends CelebrityAccountRequestFilters(null, null, null, null, null) {
      override def requireCelebrityUrlSlug(continue: (Celebrity) => Any)(implicit request: Request, session: Session) = {
        continue(celebrity)
      }

      override def requireCelebrityProductUrl(celebrity: Celebrity)(continue: (Product) => Any)(implicit request: Request, session: Session) = {
        continue(product)
      }
    }

    implicit val (request, session) = newRequestAndMockSession
    val continueCallback = mock[Function2[Celebrity, Product, Any]]
    continueCallback.apply(celebrity, product) returns(new Ok)
    val result = PassesDelegatedFilters.requireCelebrityAndProductUrlSlugs(continueCallback)

    result.isInstanceOf[Ok] should be (true)
    there was one (continueCallback).apply(celebrity, product)
  }

  "isCelebrityViewable" should "return true if celebrity is unpublished, full admin tools are enabled, and admin is logged in" in {
    implicit val (request, session) = newRequestAndMockSession
    val (celebrity, _) = setupCelebrityMocks(publishedStatus = PublishedStatus.Unpublished)
    val adminStore = mock[AdministratorStore]
    adminStore.isAdmin(None) returns true

    instance(administratorStore = adminStore, playConfig = Utils.properties("admin.tools.enabled" -> "full"))
      .isCelebrityViewable(celebrity) should be(true)
    instance(administratorStore = adminStore).isCelebrityViewable(celebrity) should be(false)
  }

  "isProductViewable" should "return true if celebrity is unpublished, full admin tools are enabled, and admin is logged in" in {
    implicit val (request, session) = newRequestAndMockSession
    val product = mock[Product]
    product.publishedStatus returns PublishedStatus.Unpublished
    val adminStore = mock[AdministratorStore]
    adminStore.isAdmin(None) returns true

    instance(administratorStore = adminStore, playConfig = Utils.properties("admin.tools.enabled" -> "full"))
      .isProductViewable(product) should be(true)
    instance(administratorStore = adminStore).isProductViewable(product) should be(false)
  }

  "isAdminToolsEnabled" should "return true if set to full, false otherwise" in {
    instance(playConfig = Utils.properties("admin.tools.enabled" -> "full")).isAdminToolsFullyEnabled should be(true)
    instance(playConfig = Utils.properties("admin.tools.enabled" -> "restricted")).isAdminToolsFullyEnabled should be(false)
  }
}