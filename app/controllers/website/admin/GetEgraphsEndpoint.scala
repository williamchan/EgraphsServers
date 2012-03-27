package controllers.website.admin

import org.squeryl.Query
import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser

private[controllers] trait GetEgraphsEndpoint {
  this: Controller =>

  protected def egraphStore: EgraphStore

  def getEgraphs = {
    val egraphsAndResults: Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)] = egraphStore.getEgraphsAndResults
    views.Application.html.admin_egraphs(egraphsAndResults)
  }
}