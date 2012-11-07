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

/**
 * Filters where basic auth credentials match a [[models.Account]] in our database.
 *
 * Otherwise returns a Forbidden.
 */
class RequireAuthenticatedAccount @Inject() (accountStore: AccountStore)
  extends Filter[RequestHeader, Account] {

  override def filter(request: RequestHeader): Either[Result, Account] = {
    val maybeResult = for (
      credentials <- BasicAuth.Credentials(request);
      account <- accountStore.authenticate(credentials.username, credentials.password).right.toOption
    ) yield {
      account
    }

    maybeResult.toRight(left = Forbidden("Email/password information was incorrect."))
  }

  // we couldn't use RequestFilter here since this does't use a form, though it does need a request.
  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Account => Action[A]): Action[A] = {
    Action(parser) { request =>
      val forbiddenOrAccount = this.filter(request)
      val forbiddenOrResult = forbiddenOrAccount.right.map { account =>
        actionFactory(account).apply(request)
      }

      forbiddenOrResult.merge
    }
  }
}
