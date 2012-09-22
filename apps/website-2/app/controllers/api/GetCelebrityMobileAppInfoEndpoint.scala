package controllers.api

import play.api.mvc.Controller
import sjson.json.Serializer
import services.http.{ ControllerMethod, CelebrityAccountRequestFilters }
import services.blobs.Blobs
import services.Time
import Time.IntsToSeconds._
import java.util.Properties
import services.http.filters.RequireAuthenticatedAccount
import services.http.filters.RequireCelebrityId

private[controllers] trait GetCelebrityMobileAppInfoEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def requireAuthenticatedAccount: RequireAuthenticatedAccount
  protected def requireCelebrityId: RequireCelebrityId
  protected def blobs: Blobs
  protected def playConfig: Properties

  private val iPadBuildVersionProp = "ipad.buildversion"

  def getCelebrityMobileAppInfo = controllerMethod() {
    requireAuthenticatedAccount() { accountRequest =>
      val action = requireCelebrityId.inAccount(accountRequest.account) { celebrityRequest =>
        val iPadBuildVersion = playConfig.getProperty(iPadBuildVersionProp)
        val s3Key = "ipad/Egraphs_" + iPadBuildVersion + ".ipa"
        val ipaUrl = blobs.getStaticResourceUrl(s3Key, 10.minutes)
        val iPadAppInfo = Map("version" -> iPadBuildVersion, "ipaURL" -> ipaUrl)
        val mobileAppInfo = Map("ipad" -> iPadAppInfo)
        Ok(Serializer.SJSON.toJSON(mobileAppInfo))
      }

      action(accountRequest)
    }
  }
}
