package models.frontend.storefront

import models.frontend.storefront.StorefrontBreadcrumb.ActiveStatus
import models.frontend.storefront.StorefrontBreadcrumb.ActiveStatus.BreadcrumbNotActive

class StorefrontBreadcrumbs(
  breadcrumbs: Seq[StorefrontBreadcrumb]
)
{
  import StorefrontBreadcrumb.ActiveStatus._
  import StorefrontBreadcrumb.CrumbChoice._

  /*def indexOfType(crumb: StorefrontBreadcrumb.CrumbChoice):Int = {
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
  
  def withUrls(crumbsToUrls: Map[StorefrontBreadcrumb.CrumbChoice, String]) = {
    val newCrumbs = for (crumb <- breadcrumbs) yield {
      crumb.copy(url=crumbsToUrls.get(crumb.crumbChoice))
    }

    new StorefrontBreadcrumbs(newCrumbs)
  }

  def withActive(toSetActive: StorefrontBreadcrumb.CrumbChoice) = {
    // Iterate by pairs, adding breadcrumbs.last to make sure that every actual
    // breadcrumb gets a chance to be the first in the pair.
    val newCrumbs = (breadcrumbs :+ breadcrumbs.last).sliding(2).map { crumbPair =>
      val newStatus = crumbPair match {
        case Seq(current, next) if current.crumbChoice == toSetActive =>

          BreadcrumbActive

        case Seq(current, next) if next.crumbChoice == toSetActive =>
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
  import StorefrontBreadcrumb.CrumbChoice._
  
  private val breadcrumbNames = Seq(
    ChoosePhoto, Personalize, Review, Checkout, Finalize    
  )

  def apply(): StorefrontBreadcrumbs = {
    new StorefrontBreadcrumbs(
      breadcrumbNames.map(name => defaultBreadcrumb(name))
    )
  }

  private def defaultBreadcrumb(name: StorefrontBreadcrumb.CrumbChoice): StorefrontBreadcrumb = {
    StorefrontBreadcrumb(name, None, BreadcrumbNotActive)
  }
}


case class StorefrontBreadcrumb(
  crumbChoice: StorefrontBreadcrumb.CrumbChoice,
  url: Option[String],
  activeStatus: ActiveStatus
) {
  import StorefrontBreadcrumb.ActiveStatus._

  def name = {
    crumbChoice.name
  }

  def isActive = {
    activeStatus == BreadcrumbActive
  }

  def isAdjacentPreviousToActive = {
    activeStatus == BreadcrumbBeforeActive
  }
}


object StorefrontBreadcrumb {
  sealed abstract class CrumbChoice(val name: String)
  object CrumbChoice {
    case object ChoosePhoto extends CrumbChoice("Choose Photo")
    case object Personalize extends CrumbChoice("Personalize")
    case object Review extends CrumbChoice("Review")
    case object Checkout extends CrumbChoice("Checkout")
    case object Finalize extends CrumbChoice("Finalize")
  }
  
  sealed trait ActiveStatus
  object ActiveStatus {
    case object BreadcrumbActive extends ActiveStatus
    case object BreadcrumbBeforeActive extends ActiveStatus
    case object BreadcrumbNotActive extends ActiveStatus
  }
}