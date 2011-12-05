package controllers

import play.mvc.Controller
import models.Celebrity
import libs.Utils
import play.mvc.Router.ActionDefinition


/**
 * Serves the website's Celebrity root page.
 */
object CelebrityController extends Controller
  with DBTransaction
  with RequiresCelebrityName
{
  /** Controller for a celebrity's home page. */
  def index = {
    views.Application.html.celebrity(celebrity, celebrity.products())
  }

  /** Returns the home page for the provided Celebrity. */
  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl(
      "CelebrityController.index",
      Map("celebrityUrlSlug" -> celebrity.urlSlug.get)
    )
  }
}