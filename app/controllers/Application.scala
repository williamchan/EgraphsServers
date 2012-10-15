package controllers

import history.WebsiteHistory
import play.api.mvc.Action
import play.api.mvc.Controller
import monitoring.website.WebsiteMonitoring
//import com.codahale.jerkson.Json
import play.api.libs.json._

object Application extends Controller {

  def index = Action { request =>
    println("request from address is " + request.remoteAddress.toString())
    //request.body.

    val historyMap = WebsiteMonitoring.getActorInfo

    val urls = utilities.Utilities.getUrls(historyMap)
    val dataPoints = utilities.Utilities.getRecentHistory(historyMap)

    val urlsAndDataPoints = (urls, dataPoints).zipped.toList

    Ok(views.html.index(urlsAndDataPoints))
  }

  def getMetrics = Action {

    val historyMap = WebsiteMonitoring.getActorInfo
    val jsonIterable = historyMap.map { case (url, data) =>
      Json.toJson(Map(
        "source" -> Json.toJson(url),
        "dataPoints" -> Json.toJson(data)
      ))
    }
    
    val jsonSeq = jsonIterable.toSeq
    val responseJson = Json.toJson(Map("metrics" -> jsonSeq))
    
    Ok(responseJson)
  }
}