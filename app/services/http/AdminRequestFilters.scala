package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import play.mvc.Scope.Session
import controllers.WebsiteControllers
import play.mvc.results.{Redirect, Forbidden}
import controllers.website.admin.GetLoginAdminEndpoint

class AdminRequestFilters @Inject()(adminStore: AdministratorStore,
                                    celebStore: CelebrityStore,
                                    egraphStore: EgraphStore,
                                    orderStore: OrderStore,
                                    accountFilters: AccountRequestFilters,
                                    productFilters: ProductQueryFilters) {

  import OptionParams.Conversions._

  def requireAdministratorLogin(continue: (Administrator) => Any)(implicit session: Session, request: Request) = {
    val adminId = session.get(WebsiteControllers.adminIdKey)
    if (adminId == null || adminId.isEmpty) {
      new Redirect(GetLoginAdminEndpoint.url().url)
    } else {
      adminStore.findById(adminId.toLong) match {
        case None => {
          session.clear()
          new Redirect(GetLoginAdminEndpoint.url().url)
        }
        case Some(admin) => continue(admin)
      }
    }
  }

  def requireCelebrity(continue: (Celebrity, Administrator) => Any)(implicit session: Session, request: Request) = {
    requireAdministratorLogin { admin =>
      request.params.getOption("celebrityId") match {
        case None =>
          new Forbidden("Valid celebrity ID was required but not provided")

        case Some(celebrityId) => {
          celebStore.findById(celebrityId.toLong) match {
            case None =>
              new Forbidden("No celebrity was found with ID " + celebrityId)

            case Some(celebrity) => {
              continue(celebrity, admin)
            }
          }
        }
      }
    }
  }

  def requireOrder(continue: (Order, Administrator) => Any)(implicit session: Session, request: Request) = {
    requireAdministratorLogin { admin =>
      request.params.getOption("orderId") match {
        case None =>
          new Forbidden("Valid order ID was required but not provided")

        case Some(orderId) => {
          orderStore.findById(orderId.toLong) match {
            case None =>
              new Forbidden("No order was found with ID " + orderId)

            case Some(order) => {
              continue(order, admin)
            }
          }
        }
      }
    }
  }

  def requireEgraph(continue: (Egraph, Administrator) => Any)(implicit session: Session, request: Request) = {
    requireAdministratorLogin { admin =>
      request.params.getOption("egraphId") match {
        case None =>
          new Forbidden("Valid egraph ID was required but not provided")

        case Some(egraphId) => {
          egraphStore.findById(egraphId.toLong) match {
            case None =>
              new Forbidden("No egraph was found with ID " + egraphId)

            case Some(egraph) => {
              continue(egraph, admin)
            }
          }
        }
      }
    }
  }

}
