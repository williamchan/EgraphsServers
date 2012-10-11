package services.http

import play.api.mvc.{Result, AnyContent, Request}
import services.logging.LoggingContext
import com.google.inject.Inject
import services.db.{TransactionIsolation, TransactionSerializable, DBSession}
import play.api.mvc.Action
import play.api.mvc.Request
import services.http.filters.RequireAuthenticityTokenFilterProvider
import filters.HttpFilters
import play.api.mvc.BodyParsers.parse
import egraphs.authtoken.AuthenticityToken
import play.api.mvc.BodyParser

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
   * @param openDatabase true that the a database connection should be managed
   *    by this ControllerMethod instance.
   * @param dbIsolation the transaction isolation with which to connect to the
   *    database if openDatabase is true
   * @param operation
   *    the code block to execute after setting up the connection resources
   * @param request
   *    the request being served
   *
   * @return the result of the `operation` code block.
   */
  def apply[A](openDatabase:Boolean=defaultOpenDatabase,
               dbIsolation: TransactionIsolation = defaultDbIsolation)
              (action: Action[A]): Action[A] =
  {
    httpsFilter {
      logging.withRequestContext {
        httpFilters.requireSessionId {
          Action(action.parser) { request => 
            if (openDatabase) {
              db.connected(dbIsolation) {
                action(request)
              }
            }
            else {
              action(request)
            }
          }
        }
      }
    }
  }
  
  def withForm[A](
    openDatabase:Boolean=defaultOpenDatabase,
    dbIsolation: TransactionIsolation = defaultDbIsolation,
    bodyParser: BodyParser[A] = parse.anyContent
  )(
    actionFactory: AuthenticityToken => Action[A]
  ): Action[A] =
  {
    val action = AuthenticityToken.makeAvailable(bodyParser)(actionFactory)
    this.apply(openDatabase, dbIsolation)(action)
  }
  
  //
  // Private members
  //
  private val defaultOpenDatabase = true
  private val defaultDbIsolation=TransactionSerializable
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
   * Performs an operation after ensuring that the post is protected by a
   * CSRF token.
   *
   * @param doCsrfCheck true that we should check for an authenticity token before
   *     performing the operation
   * @param openDatabase true that the a database connection should be managed
   *    by this ControllerMethod instance.
   * @param operation the operation to perform
   * @param request the current request
   * @param session the current session
   * @tparam A return type of Operation
   *
   * @return either the return value of the `operation` code block or
   *     a [[play.mvc.results.Forbidden]]
   */
  def apply[A](doCsrfCheck: Boolean=true, openDatabase: Boolean=true)
              (action: Action[A]): Action[A] =
  {
    controllerMethod() {
      authenticityTokenFilter(doCsrfCheck) {
        Action(action.parser) { request => 
          action(request)
        }
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
   * @param operation operation to perform
   * @param request the current request
   * @param session the current session
   * @tparam A return type of `operation`
   *
   * @return the return value of the `operation` code block or the error state of
   *     postControllerMethod
   */
  def apply[A](action: Action[A]): Action[A] = {
    postControllerMethod(doCsrfCheck=false) {
      Action(action.parser) { request =>
        action(request)
      }
    }
  }
}