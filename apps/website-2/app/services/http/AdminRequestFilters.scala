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
                                    printOrderStore: PrintOrderStore,
                                    accountFilters: AccountRequestFilters,
                                    productFilters: ProductQueryFilters) {

  import SafePlayParams.Conversions._

  def requireAdministratorLogin(continue: (Administrator) => Any)(implicit session: Session, request: Request) = {
    val adminOption = session.getLongOption(WebsiteControllers.adminIdKey).flatMap(adminId =>
      adminStore.findById(adminId)
    )
    adminOption match {
      case None => {
        session.clear()
        new Redirect(GetLoginAdminEndpoint.url().url)
      }
      case Some(admin) => continue(admin)
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

  def requirePrintOrder(continue: (PrintOrder) => Any)(implicit session: Session, request: Request) = {
    requireAdministratorLogin { admin =>
      request.params.getOption("printOrderId") match {
        case None =>
          new Forbidden("Valid PrintOrder ID was required but not provided")

        case Some(printOrderId) => {
          printOrderStore.findById(printOrderId.toLong) match {
            case None =>
              new Forbidden("No PrintOrder was found with ID " + printOrderId)

            case Some(printOrder) => continue(printOrder)
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
