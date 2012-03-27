package controllers.website.admin

import org.squeryl.Query
import play.mvc.Controller
import models._
import vbg.VBGVerifySample
import xyzmo.XyzmoVerifyUser
import services.http.AdminRequestFilters

private[controllers] trait GetCelebrityEgraphsEndpoint {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters

  protected def celebrityStore: CelebrityStore
  protected def egraphStore: EgraphStore

  def getCelebrityEgraphs = {
    adminFilters.requireCelebrity {
      (celebrity) =>
        val egraphsAndResults: Query[(Egraph, VBGVerifySample, XyzmoVerifyUser)] = egraphStore.getCelebrityEgraphsAndResults(celebrity)
        views.Application.html.admin_celebrityegraphs(celebrity, egraphsAndResults)
    }
  }
}