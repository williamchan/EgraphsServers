package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import services.http.{ControllerMethod, CelebrityAccountRequestFilters}
import services.blobs.Blobs
import services.Time
import Time.IntsToSeconds._

private[controllers] trait GetCelebrityMobileAppInfoEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def blobs: Blobs

  // These values can be persisted when we have need to support more than one version of the iPad app.
  private val version = "1_2_3_11"
  private val s3Key = "ipad/1_2_3_11/Egraphs.ipa"

  def getCelebrityMobileAppInfo = controllerMethod() {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>
      val ipaUrl = blobs.getStaticResourceUrl(s3Key, 10.minutes)
      val iPadAppInfo = Map("version" -> version, "ipaURL" -> ipaUrl)
      val mobileAppInfo = Map("ipad" -> iPadAppInfo)
      Serializer.SJSON.toJSON(mobileAppInfo)
    }
  }
}
