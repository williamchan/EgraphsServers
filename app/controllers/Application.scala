package controllers

import history.WebsiteHistory
import play.api.mvc.Action
import play.api.mvc.Controller
import monitoring.website.WebsiteMonitoring
import play.api.libs.json._
import collections.EgraphsMetric

object Application extends Controller {

  def index = Action { request =>
    Ok(views.html.index())
  }

  def getMetrics = Action {
    val metrics = WebsiteMonitoring.getMetrics
    val jsonIterable = metrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }
}