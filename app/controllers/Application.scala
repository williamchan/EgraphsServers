package controllers

import history.WebsiteHistory
import play.api.mvc.Action
import play.api.mvc.Controller
import monitoring.website.WebsiteMonitoring

object Application extends Controller {

  def index = Action {

    val cloudwatch = utilities.Utilities.getCloudWatchClient
    val result = WebsiteHistory.getHistory(cloudwatch, 60)
    // send history to html page in a convenient way
    
    // manually built history provides higher data granularity
    val historyMap = WebsiteMonitoring.getActorInfo
    println("default to String for historyMap is " + historyMap)

    Ok(views.html.index(result.toString))
  }

}