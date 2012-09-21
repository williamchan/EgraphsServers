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
import services.http.AccountRequest
import services.http.BasicAuth

// TODO: PLAY20 migration. Test and comment this summbitch.
class RequireAuthenticatedAccount @Inject() (accountStore: AccountStore) {  

  /**
   * Only performs `operation` if the request header contained basic auth credentials that matched
   * an [[models.Account]] in our database.
   * 
   * Otherwise returns a Forbidden.
   *
   * @param operation the operation to execute when the account is found.
   *
   * @return an Action that produces either Forbidden or the result of `operation`.
   */
  def apply[A](parser: BodyParser[A] = parse.anyContent)(operation: AccountRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>
      val maybeResult = for (
        credentials <- BasicAuth.Credentials(request);
        account <- accountStore.authenticate(credentials.username, credentials.password).right.toOption
      ) yield {
        operation(AccountRequest(account, request))
      }
      
      maybeResult.getOrElse(Forbidden("Email/password information was incorrect."))
    }
  }
}
