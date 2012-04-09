package services.http

import play.mvc.Http.Request
import models._
import com.google.inject.Inject
import play.mvc.Scope.Session
import controllers.WebsiteControllers
import play.mvc.results.{Redirect, Forbidden}
import controllers.website.admin.GetAdminLoginEndpoint

class AdminRequestFilters @Inject()(adminStore: AdministratorStore,
                                    celebStore: CelebrityStore,
                                    accountFilters: AccountRequestFilters,
                                    productFilters: ProductQueryFilters) {

  import OptionParams.Conversions._

  def requireAdministratorLogin(continue: (Administrator) => Any)(implicit session: Session, request: Request) = {
    val adminId = session.get(WebsiteControllers.adminIdKey)
    if (adminId == null || adminId.isEmpty) {
      new Redirect(GetAdminLoginEndpoint.url().url)
    } else {
      adminStore.findById(adminId.toLong) match {
        case None => new Forbidden("Out to the ball game")
        case Some(admin) => continue(admin)
      }
    }
  }

  def requireCelebrity(continue: (Celebrity) => Any)(implicit session: Session, request: Request) = {
    requireAdministratorLogin { adminId =>
      request.params.getOption("celebrityId") match {
        case None =>
          new Forbidden("Valid celebrity ID was required but not provided")

        case Some(celebrityId) => {
          celebStore.findById(celebrityId.toLong) match {
            case None =>
              new Forbidden("No celebrity was found with ID " + celebrityId)

            case Some(celebrity) => {
              continue(celebrity)
            }
          }
        }
      }
    }
  }

}
