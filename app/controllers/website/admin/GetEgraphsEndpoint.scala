package controllers.website.admin

import org.squeryl.Query
import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.ControllerMethod

private[controllers] trait GetEgraphsEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def egraphStore: EgraphStore

  def getEgraphs = controllerMethod() {
    val egraphsAndResults: Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)] = egraphStore.getEgraphsAndResults
    views.Application.html.admin_egraphs(egraphsAndResults)
  }
}