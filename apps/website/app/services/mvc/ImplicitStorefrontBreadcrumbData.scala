package services.mvc

import models.frontend.storefront.StorefrontBreadcrumbs

/**
 * Eventually this will carry information for populating the purchase
 * flow breadcrumbs.
 */
trait ImplicitStorefrontBreadcrumbData {
  implicit def breadcrumbs: StorefrontBreadcrumbs = {
    StorefrontBreadcrumbs()
  }
}
