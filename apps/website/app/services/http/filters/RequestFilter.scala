package services.http.filters

import play.api.data.Form
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Flash
import play.api.mvc.Results.BadRequest
import play.api.mvc.Session
import play.api.mvc.Result
import play.api.mvc.Request

/**
 * Similar to Filter, except that RequestFilter cares about whether or not the key found
 * in a request has the data required.
 */
trait RequestFilter[KeyT, RequiredT] { this: Filter[KeyT, RequiredT] =>
  // with this form we can get data from the request that will be used to get the result.  
  protected def form: Form[KeyT]

  protected def badRequest(formWithErrors: Form[KeyT]): Result = BadRequest(formatError(formWithErrors)) // override this if you want a redirect instead

  private def formatError(formWithErrors: Form[_]): String = {
    formWithErrors.errors.map(error => error.key + ": " + error.message).mkString(", ")
  }

  // The bindForm should return a form with the parameters bound to it.
  private def inForm[A](parser: BodyParser[A])(actionFactory: RequiredT => Action[A])(bindForm: Request[A] => Form[KeyT]): Action[A] = {
    Action(parser) { request =>
      bindForm(request).fold(
        formWithErrors => badRequest(formWithErrors),

        key => this.apply(key, parser)(actionFactory)(request))
    }
  }

  def filterInRequest[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    form.bindFromRequest().fold(
      formWithErrors => Left(badRequest(formWithErrors)),

      key => this.filter(key))
  }
  
  def filterInSession[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    val sessionMap = Session.serialize(request.session)
    form.bind(sessionMap).fold(
      formWithErrors => Left(badRequest(formWithErrors)),

      key => this.filter(key))
  }

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      filterInRequest(parser).fold(
        error => error,
        required => actionFactory(required)(request))
    }
  }

  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inForm(parser)(actionFactory) { implicit request =>
      val sessionMap = Session.serialize(request.session)
      form.bind(sessionMap)
    }
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

  // This is like filter-OR. So if required is in this request, then it will execute the other requirement.
  def inRequestOrUseOtherFilter[A, OtherRequiredT](required: OtherRequiredT, parser: BodyParser[A] = parse.anyContent)(otherFilter: BodyParser[A] => (OtherRequiredT => Action[A]) => Action[A])(actionFactory: OtherRequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      form.bindFromRequest().fold(
        // not found in this filter so send it through the other filter
        formWithErrors => otherFilter(parser)(actionFactory)(request),

        // found in this filter so just execute the action from the action factory
        key => (actionFactory(required))(request))
    }
  }

  // TODO: Should consider reversing the other and this function so that it makes more sense.
  // like requiresPublishedCelebrity.inSessionOr(celebrityFromId)( requiresAdminId () ) { celebrity => ... }
  def inSessionOrUseOtherFilter[A, OtherRequiredT](required: OtherRequiredT, parser: BodyParser[A] = parse.anyContent)(otherFilter: => Either[Result, OtherRequiredT])(actionFactory: OtherRequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      val sessionMap = Session.serialize(request.session)
      form.bind(sessionMap).fold(
        // not found in this filter so send it through the other filter
        formWithErrors => otherFilter.fold(
          error => error,
          otherFilterRequired => actionFactory(otherFilterRequired)(request)),

        // found in this filter so just execute the action from the action factory
        key => (actionFactory(required))(request))
    }
  }
}