package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.results.Redirect

/**
 * Endpoint for serving up the Choose Photo page
 */
private[consumer] trait GetStorefrontReviewEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData {
  this: Controller =>

  import services.mvc.CelebrityViewConversions._
  import services.mvc.ProductViewConversions._

  protected def controllerMethod: ControllerMethod

  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontReview(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod() {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celeb, product) =>
      "This is the review page. Yeah I know, doesn't look like much."
    }
  }
}