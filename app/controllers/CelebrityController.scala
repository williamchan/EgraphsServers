package controllers

import play.mvc.Controller

object CelebrityController extends Controller
  with DBTransaction
  with RequiresCelebrityName
{

  def index = {
    views.Application.html.celebrity(celebrity)
  }
}