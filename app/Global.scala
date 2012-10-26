import common.Egraphs
import play.api.Application
import play.api.GlobalSettings

object Global extends GlobalSettings {
  
  override def onStart(app: Application) = {
    val websiteMonitor = Egraphs.websiteMonitor
    val dbMonitor = Egraphs.dbMonitor
    val cacheMonitor = Egraphs.cacheMonitor
  }
}