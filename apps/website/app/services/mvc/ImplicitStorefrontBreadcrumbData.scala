package services.mvc

import models.frontend.storefront.StorefrontBreadcrumbs


trait ImplicitStorefrontBreadcrumbData {
  implicit def breadcrumbs: StorefrontBreadcrumbs = {
    StorefrontBreadcrumbs()
  }
}
