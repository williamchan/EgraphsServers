package controllers

import common.Egraphs
import monitoring.database.DBMonitor
import play.api.libs.json.Json
import play.api.mvc.Action
import play.api.mvc.Controller
import monitoring.cache.CacheMonitor
import monitoring.alarms.AlarmUpdater
import play.api.data.Form
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {

  def index = Action { request =>
    Ok(views.html.index("Monitoring"))
  }

  def websiteAvailability = Action {
    Ok(views.html.website("WebsiteAvailability"))
  }

  def dbAvailability = Action {
    Ok(views.html.db("DBAvailability"))
  }

  def cacheAvailability = Action {
    Ok(views.html.cache("CacheAvailability"))
  }

  def websiteMetrics = Action {
    val metrics = Egraphs.websiteMonitor.getMetrics
    val jsonIterable = metrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }

  def dbMetrics = Action {
    val dbMetrics = Egraphs.dbMonitor.getMetrics
    val jsonIterable = dbMetrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }

  def cacheMetrics = Action {
    val cacheMetrics = Egraphs.cacheMonitor.getMetrics
    val jsonIterable = cacheMetrics.map(metric => metric.toJson)
    val responseJson = Json.toJson(Map("metrics" -> jsonIterable))
    Ok(responseJson)
  }

  /** The controllers below are for the Sheriff admin page */
  
  def alarms = Action {
    Ok(views.html.alarms("SNSAlarms"))
  }

  def phoneUpdate = Action { request =>
    def phone = request.body.asFormUrlEncoded.get("phone")(0)
    val alarmUpdater = new AlarmUpdater
    alarmUpdater.updateAlarm(phone)
    Ok("The phone number associated with this alarm has been changed to: " + phone)
  }
}