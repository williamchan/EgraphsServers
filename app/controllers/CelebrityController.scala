package controllers

import play.mvc.Controller

/**
 * Serves the website's Celebrity root page.
 */
object CelebrityController extends Controller
  with DBTransaction
  with RequiresCelebrityName
{
  def index = {
    views.Application.html.celebrity(celebrity, celebrity.products())
  }
}