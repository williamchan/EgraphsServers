package services.http.filters

import play.api.mvc.BodyParsers.parse
import play.api.mvc.BodyParser
import play.api.mvc.Action
import play.api.mvc.Result

/**
 * Classes that implement this Filter trait guarantee that any operation it is given to execute is only
 * executed if the a corresponding object of type RequiredT is found when the filter method is called.
 *
 * So a filter that requires a celebrity from an account may have the definition:
 *   RequiredCelebrity extends Filter[Account, Celebrity]
 */
//TODO: RequiredT Should be covarient, but idk why it won't work that way.
trait Filter[KeyT, RequiredT] {
  /** a function that takes a value and maps it to an Either[[[play.api.mvc.Result]], RequiredT]
   * for example:
   * {{{
   *  override def filter[String, Account] = accountStore.findByEmail(email).toRight(left=NotFound)
   * }}}
   */
  def filter(value: KeyT): Either[Result, RequiredT]

  /**
   * Composes a provided action `actionFactory` with the filter. actionFactory will never be 
   * executed if the filter does not pass.
   */
  def apply[A](key: KeyT, parser: BodyParser[A] = parse.anyContent)(actionFactory: RequiredT => Action[A]): Action[A] = {
    filterResultToAction(filter(key), parser, actionFactory)
  }

  private def filterResultToAction[A](filterResult: Either[Result, RequiredT], parser: BodyParser[A], actionFactory: (RequiredT) => Action[A]): Action[A] = {
    Action(parser) { request =>
      filterResult.fold(
        error => error,
        required => actionFactory(required).apply(request))
    }
  }
}