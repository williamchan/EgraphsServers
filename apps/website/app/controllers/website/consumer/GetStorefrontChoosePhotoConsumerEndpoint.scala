package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

/**
 * Endpoint for serving up the Choose Photo page
 */
private[consumer] trait GetStorefrontChoosePhotoConsumerEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontChoosePhotoTiled(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celebrity, product) =>
      // Fill this with actual rendering of the choose photo page.
    }
  }
}

private[consumer] object GetStorefrontChoosePhotoConsumerEndpoint {
  object ChoosePhotoViewingOption {
    // Fill this with portrait and landscape
  }
}