package controllers

import common.Egraphs
import play.api.Application
import play.api.GlobalSettings

object Global extends ToyBox {
  // ToyBox's abstract fields
  def maybeGetLoginRoute = Some(routes.Global.getLogin)
  def maybePostLoginRoute = Some(routes.Global.postLogin)
  def maybeAssetsRoute = Some(routes.Assets.at(_))

  override def onStart(app: Application) = {
    val websiteMonitor = Egraphs.websiteMonitor
    val dbMonitor = Egraphs.dbMonitor
    val cacheMonitor = Egraphs.cacheMonitor
  }
}