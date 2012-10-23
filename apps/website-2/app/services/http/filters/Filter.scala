package services.http.filters

import models.Account
import play.api.mvc.Results._
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Action
import play.api.data.Form
import play.api.mvc.Result
import play.api.mvc.Flash

/**
 * Classes that implement this Filter trait guarantee that any operation it is given to execute is only
 * executed if the a cooresponding object of type RequiredT is found when the filter method is called.
 * 
 * So a filter that requires a celebrity from an account may have the definition:
 *   RequiredCelebrity extends Filter[Account, Celebrity]
 */
trait Filter[KeyT, RequiredT] {
  // a list of the name of the fields, this is useful for checking forms and flash.
//  protected def keys: List[String]
  protected def valueType: String //ex: Account

  // with this form we can get data from the request that will be used to get the result.  
  protected def form: Form[KeyT]

  // this is a function that takes a value and maps it to an option of T
  // for example:
  //    override def filter[String, Account] = accountStore.findByEmail(email)
  protected def filter(value: KeyT): Option[RequiredT]

  protected def notFound = NotFound(valueType + " not found")
  protected def badRequest(key: String) = BadRequest(key + " was required but not provided")

  def apply[A](key: KeyT, parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { request =>
      filter(key) match {
        case Some(required) => actionFactory(required).apply(request)
        case None => notFound
      }
    }
  }

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      form.bindFromRequest.fold(
        errors => notFound,

        key => this.apply(key, parser)(actionFactory)(request))
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

  def asEither(key: KeyT): Either[Result, RequiredT] = {
    filter(key).toRight(left=notFound)
  }
}