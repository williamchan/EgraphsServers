package controllers.website.admin

import org.squeryl.Query
import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.{AdminRequestFilters, ControllerMethod}

private[controllers] trait GetEgraphsEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters

  protected def egraphStore: EgraphStore

  def getEgraphs = controllerMethod() {
    adminFilters.requireAdministratorLogin { adminId =>
      val egraphsAndResults: Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)] = egraphStore.getEgraphsAndResults
      views.Application.admin.html.admin_egraphs(egraphsAndResults)
    }
  }
}