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
 * A source for the data used to perform a RequestFilter.
 * 
 * This trait's subtypes represent the different parts of the request from which
 * a [[services.http.filters.RequestFilter]] can read out the necessary Key types.
 **/
sealed trait Source
case object RequestSource extends Source
case object FlashSource extends Source
case object SessionSource extends Source

/**
 * Similar to Filter, except that RequestFilter cares about whether or not the key found
 * in a request has the data required.
 */
trait RequestFilter[KeyT, RequiredT] { this: Filter[KeyT, RequiredT] =>
  //
  // Abstract members
  //
  /**
   * Form that reads a KeyT out of a [[play.api.mvc.Request[_]]]. This form will be used 
   * to read from the request, flash, and session.
   */  
  protected def form: Form[KeyT]

  /**
   * Hook for handling errors in form binding.
   * 
   * Override this to provide custom [[play.api.mvc.Result]]s based on failure sources.
   **/
  protected def formFailedResult[A, S >: Source](formWithErrors: Form[KeyT], source: S)(implicit request: Request[A]): Result = BadRequest(formatError(formWithErrors))

  
  //
  // Public members
  //
  /** 
   * Returns a Result on the left if the filter was not passed with a value in the request,
   * otherwise returns a RequiredT on the right. 
   **/
  def filterInRequest[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(RequestSource, parser)
  }

  /**
   * Returns a Result on the left if the filter was not passed with a value in the session,
   * otherwise returns a RequiredT on the right. 
   **/
  def filterInSession[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(SessionSource, parser)
  }
  
  /**
   * Returns a Result on the left if the filter was not passed with a value in the flash,
   * otherwise returns a RequiredT on the right.
   */
  def filterInFlash[A](parser: BodyParser[A] = parse.anyContent)(implicit request: Request[A]): Either[Result, RequiredT] = {
    filterInSource(FlashSource, parser)
  }

  /**
   * Action-composed alternative to filterInRequest
   */
  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(RequestSource, parser)(actionFactory)
  }

  /**
   * Action-composed alternative to filterInSession
   */
  def inSession[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(SessionSource, parser)(actionFactory)
  }

  /**
   * Action-composed alternative to filterInFlash
   */
  def inFlash[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    inSource(FlashSource, parser)(actionFactory)
  }

  /**
   * Preferentially searches the flash followed by the request for a successful binding
   * of this.form. Essentially composes an OR relationship between inFlash and inRequest
   */
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

  /** 
   * Performs a filter-OR upon another filter. So if this filter does not pass, then it test 
   * the other filter before failing out.
   * 
   * TODO: Improve this API if need arises to touch this code again.
   */
  def inRequestOrUseOtherFilter[A, OtherRequiredT](required: OtherRequiredT, parser: BodyParser[A] = parse.anyContent)(otherFilter: BodyParser[A] => (OtherRequiredT => Action[A]) => Action[A])(actionFactory: OtherRequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      form.bindFromRequest().fold(
        // not found in this filter so send it through the other filter
        formWithErrors => otherFilter(parser)(actionFactory)(request),

        // found in this filter so just execute the action from the action factory
        key => (actionFactory(required))(request))
    }
  }

  /**
   * Like inRequestOrUseOtherFilter, but for the session.
   * 
   * TODO: Should consider reversing the other and this function so that it makes more sense.
   * like requiresPublishedCelebrity.inSessionOr(celebrityFromId)( requiresAdminId () ) { celebrity => ... }
   */
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
  
  
  //
  // Private members
  //
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
  
  private def inSource[A, S >: Source](source: S, parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      filterInSource(source, parser).fold(
        error => error,
        required => actionFactory(required)(request))
    }
  }
}