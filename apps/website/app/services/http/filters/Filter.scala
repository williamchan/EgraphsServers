package services.http.filters

import models.Account
import play.api.mvc.Results._
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Action
import play.api.data.Form
import play.api.mvc.Result

/**
 * Classes that implement this Filter trait guarantee that any operation it is given to execute is only
 * executed if the a corresponding object of type RequiredT is found when the filter method is called.
 * 
 * So a filter that requires a celebrity from an account may have the definition:
 *   RequiredCelebrity extends Filter[Account, Celebrity]
 */
trait Filter[KeyT, +RequiredT] {
  // this is a function that takes a value and maps it to an option of T
  // for example:
  //    override def filter[String, Account] = accountStore.findByEmail(email)
  def filter(value: KeyT): Either[Result, RequiredT]

  def apply[A](key: KeyT, parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    Action(parser) { request =>
      filter(key).fold(
        error => error,
        required => actionFactory(required).apply(request)
      )
    }
  }
}