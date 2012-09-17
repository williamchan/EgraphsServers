package controllers.api

import play.api.mvc.Controller
import sjson.json.Serializer
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}
import services.blobs.Blobs
import services.Time
import Time.IntsToSeconds._
import java.util.Properties

private[controllers] trait GetCelebrityMobileAppInfoEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def blobs: Blobs
  protected def playConfig: Properties

  private val iPadBuildVersionProp = "ipad.buildversion"

  def getCelebrityMobileAppInfo = controllerMethod() {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      val iPadBuildVersion = playConfig.getProperty(iPadBuildVersionProp)
      val s3Key = "ipad/Egraphs_" + iPadBuildVersion + ".ipa"
      val ipaUrl = blobs.getStaticResourceUrl(s3Key, 10.minutes)
      val iPadAppInfo = Map("version" -> iPadBuildVersion, "ipaURL" -> ipaUrl)
      val mobileAppInfo = Map("ipad" -> iPadAppInfo)
      Serializer.SJSON.toJSON(mobileAppInfo)
    }
  }
}
