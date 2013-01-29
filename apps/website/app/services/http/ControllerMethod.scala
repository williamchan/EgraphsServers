package services.http

import play.api.mvc.{Result, AnyContent, Request}
import services.logging.LoggingContext
import com.google.inject.Inject
import services.db.{TransactionIsolation, TransactionSerializable, DBSession}
import play.api.mvc.Action
import services.http.filters.RequireAuthenticityTokenFilterProvider
import filters.HttpFilters
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import egraphs.authtoken.AuthenticityToken
import services.db.DBSession
import services.http._
import services.db.{TransactionIsolation, TransactionSerializable, DBSession}

/**
 * Establishes an appropriate execution context for a request handler.
 *
 * Every function that handles requests should begin with a call to an instance of this
 * class or an instance of another class that delegates to it (e.g.
 * [[services.http.POSTControllerMethod]])
 *
 * @param logging used to set up an IDed logging context for the request
 * @param db used to make a DB connection for the request where appropriate
 * @param httpsFilter used to ensure that the request occurs over SSL.
 */
class ControllerMethod @Inject()(logging: LoggingContext, db: DBSession, httpsFilter: HttpsFilter, httpFilters: HttpFilters) {

  /**
   * Prepares and customizes controller method behavior. The first expression in
   * any controller methods in our codebase should be to call this function.
   *
   * @param dbSettings the ControllerDBSettings that specify whether a database connection coincides with the
   *                   lifecycle of this controller method, as well as the transaction isolation level and
   *                   whether the transaction is read-only.
   * @return the result of the `operation` code block.
   */
  def apply[A](dbSettings: ControllerDBSettings = WithDBConnection())
              (action: Action[A]): Action[A] =
  {
    httpsFilter {
      logging.withRequestContext {
        httpFilters.requireSessionId {
          Action(action.parser) { request => 
            dbSettings match {
              case WithoutDBConnection => action(request)
              case WithDBConnection(dbIsolation, isReadOnly) => db.connected(dbIsolation, isReadOnly) { action(request) }
            }
          }
        }
      }
    }
  }
  
  def withForm[A](
    dbSettings: ControllerDBSettings = WithDBConnection(readOnly = false),
    bodyParser: BodyParser[A] = parse.anyContent
  )(
    actionFactory: AuthenticityToken => Action[A]
  ): Action[A] =
  {
    val action = AuthenticityToken.makeAvailable(bodyParser)(actionFactory)
    this.apply(dbSettings)(action)
  }  
}


/**
 * A ControllerMethod with features slightly tweaked for POSTs, particularly
 * re: anti-CSRF support.
 *
 * @param controllerMethod delegate that sets up most of the context for the request
 * @param authenticityTokenFilter provider for an authenticityTokenFilter which protects
 *     against CSRF attacks.
 */
class POSTControllerMethod @Inject()(
  controllerMethod: ControllerMethod,
  authenticityTokenFilter: RequireAuthenticityTokenFilterProvider
) {

  /**
   * Performs an operation after ensuring that the post is protected by a CSRF token.
   *
   * @param doCsrfCheck true that we should check for an authenticity token before performing the operation
   * @param dbSettings the ControllerDBSettings that specify whether a database connection coincides with the
   *                   lifecycle of this controller method, as well as the transaction isolation level and
   *                   whether the transaction is read-only.
   * @tparam A return type of Operation
   * @return either the return value of the `operation` code block or a [[play.api.mvc.Results.Forbidden]]
   */
  def apply[A](doCsrfCheck: Boolean=true,
               dbSettings: ControllerDBSettings = WithDBConnection(readOnly = false))
                             (action: Action[A]): Action[A] =
  {
    controllerMethod(dbSettings = dbSettings) {
      authenticityTokenFilter(doCsrfCheck) {
        action
      }
    }
  }
}


/**
 * Deviates from POSTControllerMethod by specifying that CSRF protection is not to be used
 * as per http://stackoverflow.com/questions/2267637/what-are-some-viable-techniques-for-combining-csrf-protection-with-restful-apis
 *
 * As long as browsers never contain the Authorization header the API should be safe from CSRF.
 * And they shouldn't contain that header because browsers never hit the API.
 *
 * @param postControllerMethod delegate that sets up most of the context for the request
 */
class POSTApiControllerMethod @Inject()(postControllerMethod: POSTControllerMethod) {

  /**
   * Performs an operation after ensuring an appropriate execution context for
   * a POST to the API.
   *
   * @param dbSettings the ControllerDBSettings that specify whether a database connection coincides with the
   *                   lifecycle of this controller method, as well as the transaction isolation level and
   *                   whether the transaction is read-only.
   * @tparam A return type of `operation`
   *
   * @return the return value of the `operation` code block or the error state of postControllerMethod
   */
  def apply[A](dbSettings: ControllerDBSettings = WithDBConnection(readOnly = false))
              (action: Action[A]): Action[A] = {
    postControllerMethod(doCsrfCheck=false, dbSettings=dbSettings) {
      action
    }
  }
}
