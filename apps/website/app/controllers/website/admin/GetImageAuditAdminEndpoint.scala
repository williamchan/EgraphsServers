package controllers.website.admin

import models.Celebrity
import models.CelebrityStore
import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.mvc.celebrity.TwitterFollowersAgent
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetImageAuditAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  
  def getImageAudit = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>
          val celebrities = celebrityStore.getAll
          val unsortedCelebrityImageAuditData = for {
            celebrity <- celebrities
          } yield {
            val landingImage = celebrity.landingPageImage.fetchImage
            CelebrityImageAuditData(
              celebrity.id,
              celebrity.publicName,
              landingImage.map(_.getWidth).getOrElse(0),
              landingImage.map(_.getHeight).getOrElse(0)
            )
          }

          val celebrityImageAuditData = unsortedCelebrityImageAuditData.toList.sortWith(
            (a, b) => a.celebrityId < b.celebrityId)

          Ok(views.html.Application.admin.admin_celebrities_images_audit(celebrityImageAuditData))
        }
    }
  }
}

case class CelebrityImageAuditData(
  celebrityId: Long,
  publicName: String,
  landingImageWidth: Int,
  landingImageHeight: Int
)