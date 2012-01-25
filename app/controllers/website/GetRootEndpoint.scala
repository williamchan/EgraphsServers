package controllers.website

import play.mvc.Controller

private[controllers] trait GetRootEndpoint { this: Controller =>
  import views.Application._

  def getRootEndpoint = {
    html.index()
  }
}