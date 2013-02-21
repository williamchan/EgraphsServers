package controllers

import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.{Redirect, NotFound}
import services.logging.Logging

/** 
 * Redirects any requests that end in forward slashes and redirects them to the slashless version.
 **/
object SlayTrailingSlashesController extends Controller with Logging {
  def slayTrailingSlashes(resourcePath: String) = Action { request =>
    log("Redirecting: " + request.path + " -> " + resourcePath)
    Redirect("/" + resourcePath, request.queryString)
  }
}