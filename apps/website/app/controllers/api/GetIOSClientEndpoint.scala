package controllers.api

import play.api.mvc.Action
import play.api.mvc.Controller
import controllers.routes.ApiControllers
import services.ConsumerApplication
import play.api.libs.iteratee.Enumerator
import services.blobs.Blobs
import services.blobs.Blobs.Conversions._
import services.logging.Logging
import java.rmi.ServerError
import services.http.ControllerMethod
import services.http.WithoutDBConnection

trait GetIOSClientEndpoint { this: Controller =>
  import GetIOSClientEndpoint._
  
  protected def controllerMethod: ControllerMethod
  protected def consumerApp: ConsumerApplication
  protected def blobs: Blobs
  
  /**
   * Returns either the manifest for the current version of the iOS app as a .plist file 
   * or a redirect (303) to an itms-services:// url that instructs the iPad how to download
   * the plist for install.
   * 
   * @param redirectToItmsLink true that we should return a 303 SEE_OTHER to this same resource
   *   with a URL protocol specified as "itms-services://" rather than "http://". This is used
   *   to avoid url scrubbing that some web-based e-mail clients perform on non-standard URL
   *   protocols.
   */
  def getIOSClient(redirectToItmsLink: Boolean) = controllerMethod(WithoutDBConnection) {
    Action {
      if (redirectToItmsLink) {
        val absoluteUrl = consumerApp.getIOSClient(redirectToItmsLink=false)
        Redirect("itms-services://?action=download-manifest&url=" + absoluteUrl)
      } else {
        val maybeOk = iosClientPlist.map(plistBytes => Ok.stream(plistBytes))
        
        maybeOk.getOrElse {
          error("Failed to retrieve iOS initial download app plist at " + plistBlobKey)
          
          NotFound
        }
      }
    }
  }
  
  def iosClientPlist: Option[Enumerator[Array[Byte]]] = {
    blobs.getStaticResource(plistBlobKey).map { blob =>
      Enumerator.fromStream(blob.asInputStream)  
    }
  }
}

object GetIOSClientEndpoint extends Logging {
  val plistBlobKey = "ipad/Egraphs-initial-download.plist"
}