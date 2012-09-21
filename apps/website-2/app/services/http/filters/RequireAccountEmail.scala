package services.http.filters

import com.google.inject.Inject

import models.AccountStore
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import services.http.AccountRequest


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAccountEmail @Inject() (accountStore: AccountStore) {
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
