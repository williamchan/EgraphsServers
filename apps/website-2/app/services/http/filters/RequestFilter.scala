package services.http.filters

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Flash
import play.api.mvc.Results.BadRequest

/**
 * Similar to Filter, except that RequestFilter cares about whether or not the key found
 * in a request has the data required.
 */
trait RequestFilter[-KeyT, +RequiredT] { this: Filter[KeyT, RequiredT] =>
  // with this form we can get data from the request that will be used to get the result.  
  protected def form: Form[KeyT]

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      form.bindFromRequest.fold(
        formWithErrors => BadRequest(formatError(formWithErrors)),

        key => this.apply(key, parser)(actionFactory)(request))
    }
  }

  private def formatError(formWithErrors: Form[_]): String = {
    formWithErrors.errors.map(error => error.key + ": " + error.message).mkString(", ")
  }

  def inFlashOrRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { request =>
      // flash takes precedence over request args
      val flashMap = Flash.serialize(request.flash)
      val maybeKey = form.bind(flashMap).fold(
        errors => None,
        key => Some(key))

      maybeKey match {
        case None => this.inRequest(parser)(actionFactory)(request)
        case Some(key) => this.apply(key, parser)(actionFactory)(request)
      }
    }
  }
}