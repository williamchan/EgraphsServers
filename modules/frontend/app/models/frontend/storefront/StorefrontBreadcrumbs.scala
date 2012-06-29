package models.frontend.storefront

import models.frontend.storefront.StorefrontBreadcrumb.ActiveStatus
import models.frontend.storefront.StorefrontBreadcrumb.ActiveStatus.BreadcrumbNotActive

class StorefrontBreadcrumbs(
  breadcrumbs: Seq[StorefrontBreadcrumb]
)
{
  import StorefrontBreadcrumb.ActiveStatus._
  import StorefrontBreadcrumb.Crumb._

  /*def indexOfType(crumb: StorefrontBreadcrumb.Crumb):Int = {
    val index = crumb match {
      case ChoosePhoto => 0
      case Personalize  => 1
      case Review => 2
      case Checkout => 3
      case Finalize => 4
    }

    index
  }*/
  
  def indexed:Seq[(StorefrontBreadcrumb, Int)] = {
    breadcrumbs.zipWithIndex
  }
  
  def withUrls(crumbsToUrls: Map[StorefrontBreadcrumb.Crumb, String]) = {
    val newCrumbs = for (crumb <- breadcrumbs) yield {
      crumb.copy(url=crumbsToUrls.get(crumb.crumbType))
    }

    new StorefrontBreadcrumbs(newCrumbs)
  }

  def withActive(toSetActive: StorefrontBreadcrumb.Crumb) = {
    // Iterate by pairs, adding breadcrumbs.last to make sure that every actual
    // breadcrumb gets a chance to be the first in the pair.
    val newCrumbs = (breadcrumbs :+ breadcrumbs.last).sliding(2).map { crumbPair =>
      println("pair is -- " + crumbPair.map(_.crumbType).mkString(", "))
      val newStatus = crumbPair match {
        case Seq(current, next) if current.crumbType == toSetActive =>

          BreadcrumbActive

        case Seq(current, next) if next.crumbType == toSetActive =>
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
    StorefrontBreadcrumb(name, None, BreadcrumbNotActive)
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