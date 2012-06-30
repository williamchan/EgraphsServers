package controllers.website.consumer

import services.mvc.{ImplicitStorefrontBreadcrumbData, ImplicitHeaderAndFooterData}
import play.mvc.Controller

trait PostStorefrontChoosePhotoConsumerEndpoint
  extends ImplicitHeaderAndFooterData
  with ImplicitStorefrontBreadcrumbData
{ this: Controller =>

  def postStorefrontChoosePhoto(celebrityUrlSlug: String, productUrlSlug: String) = {

  }

  def lookupPostChoosePhoto(celebrityUrlSlug: String, productUrlSlug: String) = {
    reverse(this.postStorefrontChoosePhoto(celebrityUrlSlug, productUrlSlug))
  }
}
