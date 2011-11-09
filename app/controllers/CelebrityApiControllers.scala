package controllers

import play.mvc.Controller
import sjson.json.Serializer

object CelebrityApiControllers extends Controller
  with RequiresAuthenticatedAccount
  with RequiresCelebrity
{

  def getCelebrity = {
    Serializer.SJSON.toJSON(celebrity.renderedForApi)
  }
}