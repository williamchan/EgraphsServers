package controllers

import play.api.mvc.{Action, Controller}

object Error extends Controller {

  def badRequest() = Action {
    BadRequest(views.html.frontend.errors.bad_request())
  }

  def error() = Action {
    InternalServerError(views.html.frontend.errors.error())
  }

  def forbidden() = Action {
    Forbidden(views.html.frontend.errors.forbidden())
  }

  def notFound() = Action {
    NotFound(views.html.frontend.errors.not_found())
  }
}
