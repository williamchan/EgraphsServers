package services.logging

import org.slf4j.MDC
import play.mvc.Http.Request
import services.http.RequestInfo

/**
 * Allows functions to be performed within a logging "Context", where
 * all the logs that originate during the context carry the contextual information, making
 * the log file more easily navigable.
 *
 * It does this by manipulating the MDC map, which is backed by a ThreadLocal in log4j. So if we move
 * to some sort of async approach we'll have to be pretty careful about depending on the values being correct.
 */
class LoggingContext {
  /** Key into the MDC */
  private val contextKey = "context"
  private val defaultContext = "<No context>"

  /** Opens up the default context */
  def bootstrap() {
    MDC.put(contextKey, defaultContext)
  }

  /**
   * Opens up a logging context for the provided request. Any logs generated during the
   * request can be traced back to this IP address and request. It identifies the user and request
   * using [[services.http.RequestInfo]]
   *
   * @param operation block of code that should execute within the request logging context
   * @return the result of the operation
   **/
  def withContext[A](request: Request)(operation: => A): A = {
    val requestInfo = new RequestInfo(request)

    // Log the request header
    MDC.put(contextKey, "")
    val requestHeader = new StringBuilder("==== ")
      .append("Serving IP ")
      .append(request.remoteAddress)
      .append("(id=")
      .append(requestInfo.clientId)
      .append(", ")
      .append("requestId=")
      .append(requestInfo.requestId)
      .append(") with ")
      .append(request.actionMethod)
      .append(" ====")

    play.Logger.info(requestHeader.toString())

    // Prepare the context for any logs that occur after this point
    val requestContext = new StringBuilder("<")
      .append(request.actionMethod)
      .append("(")
      .append(requestInfo.clientId)
      .append(".")
      .append(requestInfo.requestId)
      .append(")")
      .append(">")

    withContext(requestContext.toString())(operation)
  }

  /**
   * Opens up a logging context. Any logs generated during the context will also
   * feature the contents of the context parameter.
   *
   * @param context message that should appear in all logs during the request
   * @param operation block of code that should execute within the context
   * @return the result of the operation
   */
  def withContext[A](context: String)(operation: => A): A = {
    MDC.put(contextKey, context)
    try {
      operation
    }
    finally {
      MDC.put(contextKey, defaultContext)
    }
  }
}

