package controllers.api

import scala.language.postfixOps
import play.api.libs.json._
import play.api.mvc.Action
import play.api.mvc.Controller
import services.Time.IntsToSeconds.intsToSecondDurations
import services.blobs.Blobs
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId
import services.config.ConfigFileProxy

private[controllers] trait GetCelebrityMobileAppInfoEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def blobs: Blobs
  protected def config: ConfigFileProxy

  private val iPadBuildVersionProp = "ipad.buildversion"

  def getCelebrityMobileAppInfo = controllerMethod() {
    httpFilters.requireAuthenticatedAccount.inRequest() { account =>
      httpFilters.requireCelebrityId.inAccount(account) { celebrity =>
        Action {
          val iPadBuildVersion = config.ipadBuildVersion
          val s3Key = "ipad/Egraphs_" + iPadBuildVersion + ".ipa"
          val ipaUrl = blobs.getStaticResourceUrl(s3Key, 10 minutes)
          val mobileAppInfo = Json.obj(
            "ipad" -> Json.obj(
              "version" -> iPadBuildVersion,
              "ipaURL" -> ipaUrl
            )
          )
          Ok(mobileAppInfo)
        }
      }
    }
  }
}
