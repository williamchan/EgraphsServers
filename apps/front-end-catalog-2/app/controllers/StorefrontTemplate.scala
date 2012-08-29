package controllers

import models.frontend.storefront.{StorefrontBreadcrumb, StorefrontBreadcrumbs}
import play.mvc.Controller

object StorefrontTemplate extends Controller with DefaultHeaderAndFooterData {
  import StorefrontBreadcrumb.CrumbChoice._

  def noneActiveOrComplete = {
    implicit val breadcrumbs = defaultBreadcrumbs

    views.html.uses_storefront_template()
  }

  def allComplete = {
    implicit val breadcrumbs = breadcrumbsWithAllComplete

    views.html.uses_storefront_template()
  }

  def allCompleteMiddleOneActive = {
    implicit val breadcrumbs = breadcrumbsWithAllComplete.withActive(Review)

    views.html.uses_storefront_template()
  }

  def active(crumbIndex: Int) = {
    implicit val crumbs = defaultBreadcrumbs.withActive(crumbTypeFromIndex(crumbIndex))

    views.html.uses_storefront_template()
  }

  val defaultBreadcrumbs = {
    StorefrontBreadcrumbs()
  }

  //
  // Private members
  //
  private val breadcrumbsWithAllComplete: StorefrontBreadcrumbs = {
    defaultBreadcrumbs.withUrls(
      Map(
        ChoosePhoto -> "/choose-photo",
        Personalize -> "/personalize",
        Review -> "/review",
        Checkout -> "/checkout",
        Finalize -> "finalize"
      )
    )
  }

  private def crumbTypeFromIndex(crumbIndex: Int) = {
    val lookup = Map(
      1 -> ChoosePhoto,
      2 -> Personalize,
      3 -> Review,
      4 -> Checkout,
      5 -> Finalize
    )

    lookup(crumbIndex)
  }
}


trait DefaultStorefrontBreadcrumbs {
  implicit val defaultBreadcrumbs = {
    StorefrontTemplate.defaultBreadcrumbs
  }
}