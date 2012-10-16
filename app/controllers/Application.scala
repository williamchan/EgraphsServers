package controllers

import history.WebsiteHistory
import play.api.mvc.Action
import play.api.mvc.Controller
import monitoring.website.WebsiteMonitoring
import play.api.libs.json._
import collections.EgraphsMetric

object Application extends Controller {

  def index = Action { request =>

    val metrics = WebsiteMonitoring.getMetrics

//    val names = utilities.Utilities.getNames(metrics)
//    val urls = utilities.Utilities.getDescriptions(metrics)
//    val dataPoints = utilities.Utilities.getValues(metrics)
//
//    val urlsAndDataPoints = (urls, dataPoints).zipped.toList

    Ok(views.html.index(metrics))
  }

  def getMetrics = Action {

    val metrics = WebsiteMonitoring.getMetrics
    val jsonIterable = metrics.map { case EgraphsMetric(name, description, values) =>
      Json.toJson(Map(
        "sourceName" -> Json.toJson(name),
        "description" -> Json.toJson(description),
        "dataPoints" -> Json.toJson(values.toArray)
      ))
    }
    
    val jsonSeq = jsonIterable
    val responseJson = Json.toJson(Map("metrics" -> jsonSeq))
    
    Ok(responseJson)
  }
}