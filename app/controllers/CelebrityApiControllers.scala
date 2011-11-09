package controllers

import play.mvc.Controller
import sjson.json.Serializer

/**
 * Controllers that handle direct API requests for celebrity resources.
 */
object CelebrityApiControllers extends Controller
  with RequiresAuthenticatedAccount
  with RequiresCelebrity
{

  def getCelebrity = {
    Serializer.SJSON.toJSON(celebrity.renderedForApi)
  }
}