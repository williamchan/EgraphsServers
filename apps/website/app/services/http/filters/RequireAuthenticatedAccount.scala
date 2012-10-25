package services.http.filters

import com.google.inject.Inject
import models.Account
import models.AccountStore
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.WrappedRequest
import play.api.mvc.Results.Forbidden
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import services.http.BasicAuth
import play.api.mvc.RequestHeader

// TODO: PLAY20 migration. Comment this summbitch.
class RequireAuthenticatedAccount @Inject() (accountStore: AccountStore) {  

  /**
   * Only performs `operation` if the request header contained basic auth credentials that matched
   * an [[models.Account]] in our database.
   * 
   * Otherwise returns a Forbidden.
   *
   * @param actionFactory returns the action to execute when the account is found.
   *
   * @return an Action that produces either Forbidden or the result of `operation`.
   */
  def apply[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Account => Action[A])
  : Action[A] = 
  {
    Action(parser) { request =>
      val forbiddenOrAccount = this.asEither(request)
      val forbiddenOrResult = forbiddenOrAccount.right.map { account => 
        actionFactory(account).apply(request)
      }
      
      forbiddenOrResult.fold(forbidden => forbidden, result => result)      
    }
  }
  
  def asEither(request: RequestHeader): Either[Result, Account] = {
    val maybeResult = for (
      credentials <- BasicAuth.Credentials(request);
      account <- accountStore.authenticate(credentials.username, credentials.password).right.toOption
    ) yield {
      account
    }
    
    maybeResult.toRight(left=Forbidden("Email/password information was incorrect."))
  }
}