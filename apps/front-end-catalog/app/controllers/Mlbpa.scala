package controllers

import play.api.mvc.{Action, Controller}
import helpers.DefaultImplicitTemplateParameters
import models.frontend.egraph.MlbpaEgraphView
import models.frontend.PaginationInfo

object Mlbpa extends Controller with DefaultImplicitTemplateParameters {

  def egraphs() = Action {
    implicit val paginationInfo = PaginationInfo(showPaging = false, "- 0", None, None, None, None)
    Ok(views.html.frontend.mlbpa.mlbpa_egraphs(
      List(MlbpaEgraphView(
        celebrityName = "Arnold Schwarzenegger",
        egraphId = 1,
        egraphMp4Url = "https://d3kp0rxeqzwisk.cloudfront.net/egraphs/50/video/width-600px-v0.mp4"
      ))
    ))
  }
}
