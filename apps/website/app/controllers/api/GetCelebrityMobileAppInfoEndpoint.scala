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

  def getCelebrityMobileAppInfo = controllerMethod() {
    celebFilters.requireCelebrityAccount { (account, celebrity) =>

      // Need a table to track what the most-recent version of the iPad app is.
      // Table called MobileAppVersion with columns: Device, Version, Key.
      // Celebrity gets a mobileAppVersionId FK. None if this API should return the latest. Else, point to a specific MobileAppVersion.
      val version = "1_2_3_11"
      val key = "ipad/1_2_3_11/Egraphs.ipa"

      val ipaUrl = blobs.getStaticResourceUrl(key, 1.hour)
      val iPadAppInfo = Map("version" -> version, "ipaURL" -> ipaUrl)
      val mobileAppInfo = Map("ipad" -> iPadAppInfo)
      Serializer.SJSON.toJSON(mobileAppInfo)
    }
  }
}
