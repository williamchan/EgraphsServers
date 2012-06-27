package controllers.website.consumer

import services.http.{CelebrityAccountRequestFilters, ControllerMethod}
import play.mvc.Controller

private [consumer] trait GetStorefrontChoosePhotoConsumerEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters

  def getStorefrontChoosePhotoTiled(celebrityUrlSlug: String, productUrlSlug: String) = controllerMethod()
  {
    celebFilters.requireCelebrityAndProductUrlSlugs { (celebrity, product) =>

    }
  }
}

object getStorefrontChoosePhotoConsumerEndpoint {
  object ChoosePhotoViewingOption = {

  }
}