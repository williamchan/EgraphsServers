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

sealed trait Source
case class RequestSource() extends Source
case class FlashSource() extends Source
case class SessionSource() extends Source

/**
 * Similar to Filter, except that RequestFilter cares about whether or not the key found
 * in a request has the data required.
 */
trait RequestFilter[KeyT, RequiredT] { this: Filter[KeyT, RequiredT] =>
  // with this form we can get data from the request that will be used to get the result.  
  protected def form: Form[KeyT]

  // override this if you want a redirect instead or change the result by modifying session or something
  protected def formFailedResult[A, S >: Source](formWithErrors: Form[KeyT], source: S)(implicit request: Request[A]): Result = BadRequest(formatError(formWithErrors))

  private def formatError(formWithErrors: Form[_]): String = {
    formWithErrors.errors.map(error => error.key + ": " + error.message).mkString(", ")
  }

  private def bindForm[A, S >: Source](source: S)(implicit request: Request[A]): Form[KeyT] = {
    source match {
      case RequestSource =>
        form.bindFromRequest()
      case SessionSource =>
        val sessionMap = Session.serialize(request.session)
        form.bind(sessionMap)
      case FlashSource =>
        val flashMap = Flash.serialize(request.flash)
        form.bind(flashMap)
    }
  }

  private def filterInSource[A, S >: Source](source: S, parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    bindForm(source).fold(
      formWithErrors => Left(formFailedResult(formWithErrors, source)),

      key => this.filter(key))
  }

  def filterInRequest[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(RequestSource, parser)
  }

  def filterInSession[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(SessionSource, parser)
  }

  def filterInFlash[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(FlashSource, parser)
  }

  private def inSource[A, S >: Source](source: S, parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      filterInSource(source, parser).fold(
        error => error,
        required => actionFactory(required)(request))
    }
  }
  
  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(RequestSource, parser)(actionFactory)
  }

  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(SessionSource, parser)(actionFactory)
  }

  def inFlash[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(FlashSource, parser)(actionFactory)
  }

  def inFlashOrRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      // flash takes precedence over request args
      val maybeKey = bindForm(FlashSource).fold(
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