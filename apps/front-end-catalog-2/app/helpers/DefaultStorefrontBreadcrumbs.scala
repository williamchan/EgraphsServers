package helpers

import models.frontend.storefront.StorefrontBreadcrumbs

trait DefaultStorefrontBreadcrumbs {
  implicit val defaultBreadcrumbs: StorefrontBreadcrumbs = {
    StorefrontBreadcrumbs()
  }
}