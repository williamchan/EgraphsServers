package services.http

import play.mvc.Http.Request
import services.logging.LoggingContext
import com.google.inject.Inject
import services.db.{TransactionIsolation, TransactionSerializable, DBSession}
import play.mvc.Scope.Session

/**
 * Establishes an appropriate execution context for a request.
 * Every function that handles requests begin with a call to an instance of this class
 * or an instance of another class that delegates to it.
 */
class ControllerMethod @Inject()(logging: LoggingContext, db: DBSession, httpsFilter: HttpsFilter) {

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
  def apply[A](openDatabase:Boolean=true,
               dbIsolation: TransactionIsolation = TransactionSerializable)
              (operation: => A)
              (implicit request: Request): Any =
  {
    val redirectOrResult = httpsFilter {
      logging.withContext(request) {
        if (openDatabase) {
          db.connected(dbIsolation) {
            operation
          }
        }
        else {
          operation
        }
      }
    }

    redirectOrResult.fold(error => error, result => result)
  }
}


/**
 * A ControllerMethod with features slightly tweaked for POSTs, particularly
 * re: anti-CSRF support.
 *
 * @param controllerMethod delegate that sets up most context for the request
 * @param authenticityTokenFilter provider for an authenticityTokenFilter.
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
   * @param operation the operation to perform
   * @param request the current request
   * @param session the current session
   * @tparam A return type of Operation
   *
   */
  def apply[A](doCsrfCheck: Boolean = true)(operation: => A)(implicit request: Request, session: Session): Any = {
    controllerMethod() {
      authenticityTokenFilter(doCsrfCheck) {
        operation
      }.fold(forbidden => forbidden, result => result)
    }
  }
}


/**
 * Deviates from POSTControllerMethod in that it specifies that CSRF protection is not to be used
 * as per http://stackoverflow.com/questions/2267637/what-are-some-viable-techniques-for-combining-csrf-protection-with-restful-apis
 *
 * As long as browsers never contain the Authorization header we should be safe from CSRF.
 * And they shouldn't contain that header because browsers never hit the API.
 *
 * @param postControllerMethod
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
   * @return
   */
  def apply[A](operation: => A)(implicit request: Request, session: Session): Any = {
    postControllerMethod(doCsrfCheck=false) {
      operation
    }
  }
}