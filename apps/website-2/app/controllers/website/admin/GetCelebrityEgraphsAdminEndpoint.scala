package controllers.website.admin

import play.api.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import controllers.WebsiteControllers
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory

private[controllers] trait GetCelebrityEgraphsAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore
  
  import services.AppConfig.instance
  private def egraphQueryFilters = instance[EgraphQueryFilters]

  def getCelebrityEgraphsAdmin(celebrityId: Long, filter: String = "pendingAdminReview", page: Int = 1) = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>
          val query = filter match {
            case "passedBiometrics" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.passedBiometrics)
            case "failedBiometrics" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.failedBiometrics)
            case "approvedByAdmin" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.approvedByAdmin)
            case "rejectedByAdmin" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.rejectedByAdmin)
            case "published" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.published)
            case "awaitingVerification" => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.awaitingVerification)
            case "all" => egraphStore.getCelebrityEgraphsAndResults(celebrity)
            case _ => egraphStore.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.pendingAdminReview)
          }
            val pagedQuery: (Iterable[(Egraph, Option[VBGVerifySample], Option[XyzmoVerifyUser])], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
            implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetOrdersAdminEndpoint.url)
            Ok(views.html.Application.admin.admin_egraphs(egraphAndResults = pagedQuery._1, celebrity = Some(celebrity)))
            }
      }
    }
  }
}

object GetCelebrityEgraphsAdminEndpoint {

  def url(celebrity: Celebrity) = {
    controllers.routes.WebsiteControllers.getCelebrityEgraphsAdmin(celebrity.id).url
  }
}
