package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import history.WebsiteHistory

object Application extends Controller {

  def index = Action {

    val cloudwatch = utilities.Utilities.getCloudWatchClient
    val result = WebsiteHistory.getHistory(cloudwatch, 60)
    // send history to html page in a convenient way

    Ok(views.html.index(result.toString))
  }

}