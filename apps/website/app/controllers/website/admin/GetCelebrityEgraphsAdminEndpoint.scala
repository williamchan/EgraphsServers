package controllers.website.admin

import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import controllers.WebsiteControllers
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.{Ok, Redirect}
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import controllers.PaginationInfoFactory

import services.mvc.{celebrity, ImplicitHeaderAndFooterData}

private[controllers] trait GetCelebrityEgraphsAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters

  def getCelebrityEgraphsAdmin(celebrityId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId) { (celebrity) =>
        Action { implicit request =>
          
          // get query parameters
          val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
          val filter: String = Form("filter" -> text).bindFromRequest.fold(formWithErrors => "pendingAdminReview", validForm => validForm)
          
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
            val pagedQuery: (Iterable[(Egraph, Celebrity, Option[VBGVerifySample], Option[XyzmoVerifyUser])], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
            implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = GetCelebrityEgraphsAdminEndpoint.url(celebrity), filter = Option(filter))
            Ok(views.html.Application.admin.admin_egraphs(egraphsAndResults = pagedQuery._1, celebrity = Some(celebrity)))
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
