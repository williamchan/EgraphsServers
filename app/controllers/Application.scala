package controllers

import common.Egraphs
import monitoring.database.DBMonitor
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller

object Application extends Controller {

  def index = Action { request =>
    Ok(views.html.index())
  }

  def getMetrics = Action {
    val metrics = Egraphs.websiteMonitor.getMetrics
    val jsonIterable = metrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }
  
  def getDBAvailability = Action {
    val dbMetrics = Egraphs.dbMonitor.getMetrics
    val jsonIterable = dbMetrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }
  
  def love = Action {
    Ok(views.html.db())
  }
  
  // testing purposes only
  def ok = Action{Ok}
}