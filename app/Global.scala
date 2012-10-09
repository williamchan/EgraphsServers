import play.api._
import monitoring.website.WebsiteMonitoring

object Global extends GlobalSettings {
  
  override def onStart(app: Application) {
    WebsiteMonitoring.init()
  }
}