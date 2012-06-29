package models.frontend.storefront

import models.frontend.storefront.StorefrontBreadcrumb.ActiveStatus
import play.templates.Html

class StorefrontBreadcrumbs(
  breadcrumbs: Seq[StorefrontBreadcrumb]
)
{
  import StorefrontBreadcrumb.ActiveStatus._

  def indexed:Seq[(StorefrontBreadcrumb, Int)] = {
    breadcrumbs.zipWithIndex
  }

  def withActive(toSetActive: StorefrontBreadcrumb.Crumb) = {
    val newCrumbs = breadcrumbs.grouped(2).map { crumbPair =>
      val newStatus = crumbPair match {
        case Seq(current, next) if current.crumbType == toSetActive =>
          BreadcrumbActive

        case Seq(current, next) if current.crumbType == toSetActive =>
          BreadcrumbBeforeActive

        case _ =>
          BreadcrumbNotActive
      }

      crumbPair.head.copy(activeStatus=newStatus)
    }

    new StorefrontBreadcrumbs(newCrumbs.toSeq)
  }
}


object StorefrontBreadcrumbs {
  import StorefrontBreadcrumb.Crumb._
  
  private val breadcrumbNames = Seq(
    ChoosePhoto, Personalize, Review, Checkout, Finalize    
  )

  def apply(): StorefrontBreadcrumbs = {
    new StorefrontBreadcrumbs(
      breadcrumbNames.map(name => defaultBreadcrumb(name))
    )
  }

  private def defaultBreadcrumb(name: StorefrontBreadcrumb.Crumb): StorefrontBreadcrumb = {
    StorefrontBreadcrumb(name, None, ActiveStatus.BreadcrumbActive)
  }
}


case class StorefrontBreadcrumb(
  crumbType: StorefrontBreadcrumb.Crumb,
  url: Option[String],
  activeStatus: ActiveStatus
) {
  import StorefrontBreadcrumb.ActiveStatus._

  def name = {
    crumbType.name
  }

  def isActive = {
    activeStatus == BreadcrumbActive
  }

  def isAdjacentPreviousToActive = {
    activeStatus == BreadcrumbBeforeActive
  }
}


object StorefrontBreadcrumb {
  sealed abstract class Crumb(val name: String)
  object Crumb {
    case object ChoosePhoto extends Crumb("Choose Photo")
    case object Personalize extends Crumb("Personalize")
    case object Review extends Crumb("Review")
    case object Checkout extends Crumb("Checkout")
    case object Finalize extends Crumb("Finalize")
  }
  
  sealed trait ActiveStatus
  object ActiveStatus {
    case object BreadcrumbActive extends ActiveStatus
    case object BreadcrumbBeforeActive extends ActiveStatus
    case object BreadcrumbNotActive extends ActiveStatus
  }
}