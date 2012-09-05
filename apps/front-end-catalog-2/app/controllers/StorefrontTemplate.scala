package controllers

import models.frontend.storefront.{StorefrontBreadcrumb, StorefrontBreadcrumbs}
import play.api._
import play.api.mvc._

object StorefrontTemplate extends Controller with DefaultHeaderAndFooterData {
  import StorefrontBreadcrumb.CrumbChoice._

  def noneActiveOrComplete = Action {
    implicit val breadcrumbs = defaultBreadcrumbs

    Ok(views.html.uses_storefront_template())
  }

  def allComplete = Action {
    implicit val breadcrumbs = breadcrumbsWithAllComplete

    Ok(views.html.uses_storefront_template())
  }

  def allCompleteMiddleOneActive = Action {
    implicit val breadcrumbs = breadcrumbsWithAllComplete.withActive(Review)

    Ok(views.html.uses_storefront_template())
  }

  def active(crumbIndex: Int) = Action {
    implicit val crumbs = defaultBreadcrumbs.withActive(crumbTypeFromIndex(crumbIndex))

    Ok(views.html.uses_storefront_template())
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