package controllers.website.admin

import services.http.ControllerMethod
import models.filters.{FilterValueStore, FilterStore}
import play.mvc.Controller
import play.mvc.results.NotFound
import models.CelebrityStore

private[controllers] trait GetFilterAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def celebrityStore: CelebrityStore
  protected def filterStore: FilterStore
  protected def filterValueStore: FilterValueStore

  def getFilters = new NotFound("Not implemented")
}
