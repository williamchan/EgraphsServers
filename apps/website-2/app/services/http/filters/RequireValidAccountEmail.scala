package services.http

import com.google.inject.Inject

import models.Account
import models.AccountStore
import play.api.mvc.Action
import play.api.mvc.Results.{NotFound, Forbidden}
import play.api.mvc.AnyContent
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import play.api.mvc.WrappedRequest


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireValidAccountEmail @Inject() (accountStore: AccountStore) {
  def apply[A](email: String, parser: BodyParser[A] = parse.anyContent)(operation: AccountRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>
      accountStore.findByEmail(email) match {
        case Some(account) => operation(AccountRequest(account, request))
        case None => NotFound("Account not found")
      }
    }
  }
}