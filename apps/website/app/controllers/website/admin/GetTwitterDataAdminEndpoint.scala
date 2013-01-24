package controllers.website.admin

import models.Celebrity
import models.CelebrityStore
import play.api.mvc.Controller
import play.api.mvc.Action
import services.http.filters.HttpFilters
import services.http.ControllerMethod
import services.mvc.celebrity.TwitterFollowersAgent
import services.mvc.ImplicitHeaderAndFooterData

private[controllers] trait GetTwitterDataAdminEndpoint extends ImplicitHeaderAndFooterData { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters

  protected def celebrityStore: CelebrityStore

  def getTwitterData = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() {
      case (admin, adminAccount) =>
        Action { implicit request =>
          val celebrities = celebrityStore.getAll

          val twitterFollowersCounts = TwitterFollowersAgent.singleton().withDefaultValue(0)

          val celebrityTwitterDataUnsorted = for {
            celebrity <- celebrities
          } yield {
            CelebrityTwitterData(celebrity.id, celebrity.publicName, celebrity.twitterUsername, twitterFollowersCounts(celebrity.id))
          }

          val celebrityTwitterData = celebrityTwitterDataUnsorted.toList.sortWith(
            (a, b) => a.celebrityId < b.celebrityId).sortWith(
              (a, b) => a.twitterFollowersCount > b.twitterFollowersCount)

          Ok(
            views.html.Application.admin.admin_celebrities_twitter(celebrityTwitterData))
        }
    }
  }
}

case class CelebrityTwitterData(
  celebrityId: Long,
  publicName: String,
  officialScreenName: Option[String],
  twitterFollowersCount: Int)