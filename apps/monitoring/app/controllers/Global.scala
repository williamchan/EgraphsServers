package controllers

import common.Egraphs
import play.api.GlobalSettings

object Global extends ToyBox {
  // ToyBox's abstract fields
  override val loginPath = "/toybox/login"
  override val assetsRoute = routes.Assets.at(_)

  override def onStart(app: play.api.Application) = {
    val websiteMonitor = Egraphs.websiteMonitor
    val dbMonitor = Egraphs.dbMonitor
    val cacheMonitor = Egraphs.cacheMonitor
  }
}