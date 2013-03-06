package services.logging

import org.slf4j.MDC
import play.api.mvc.{AnyContent, Request}
import services.http.RequestInfo
import play.api.Logger.info
import services.{Utils, Time}
import play.api.mvc.Action
import controllers.routes
import services.crypto.Crypto
import services.http.EgraphsSession.Conversions._

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
   * @param action block of code that should execute within the request logging context
   * @return the result of the operation
   **/
  def withRequestContext[A](action: Action[A]): Action[A] = {
    Action(action.parser) { request =>
      val requestInfo = new RequestInfo(request)
  
      // Prepare the context for any logs that occur after this point
      val requestContext = createRequestContext(request, requestInfo)
  
      withContext(requestContext) {
        try {
          logRequestHeader(request, requestInfo)
          action(request)
        }
        catch {
          case e: Exception =>
            info("Exception raised during request. Request details follow. ")
            logRequestDetails(request)
            throw e
        }
      }
    }
  }

  /**
   * Opens a logging context with a single, unique-ish ID that gets tagged onto the name.
   * Use this for logging in Jobs or in bootstrap.
   *
   * @param name name of the logging context
   * @param operation operation to execute within the context
   * @tparam A return type of operation
   *
   * @return the return value of operation
   */
  def withTraceableContext[A](name: String)(operation: => A): A = {    
    val idSeed = new StringBuilder(name)
      .append(services.Random.string(10))
      .append(Time.now)
    
    val id = Crypto.MD5.hash(idSeed.toString()).substring(0, 9)

    val contextString = new StringBuilder(name).append("(").append(id).append(")").toString()

    withContext(contextString) {
      operation
    }
  }
  
  private def createRequestContext(request: Request[_], requestInfo: RequestInfo): String = {
    try {
      new StringBuilder()
        .append("(")
        .append(requestInfo.clientId)
        .append(".")
        .append(requestInfo.requestId)
        .append(")")
        .toString()
    }
    catch {
      case e: Exception => 
        play.api.Logger.error("[LoggingContext] Failed to generate request context string due to " + e.getClass.getName)
        "<Unidentifiable request context>"
    }
  }
  
  private def logRequestHeader(request: Request[_], requestInfo: RequestInfo) {
    try {
      val requestHeader = new StringBuilder("Serving \"")
        .append(request.method)
        .append(" ")
        .append(request.uri)
        .append("\" ")
        .append("to session ")
        .append(request.session.id.getOrElse("(no session ID)"))
        .append(" (id=")
        .append(requestInfo.clientId)
        .append(", ")
        .append("requestId=")
        .append(requestInfo.requestId)
        .append(")")
        .append("  User-Agent: ")
        .append(request.headers.get("User-Agent").getOrElse("Not provided"))
      
      info(requestHeader.toString())
    }
    catch {
      case e: Exception =>
        play.api.Logger.error("[LoggingContext] Failed to generate request header due to " + e.getClass.getName)
    }
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
    val oldContext = Option(MDC.get(contextKey)).getOrElse(defaultContext)

    MDC.put(contextKey, context)
    try {
      operation
    }
    catch {
      case e: Exception =>
        Utils.logException(e)
        throw e
    }
    finally {
      MDC.put(contextKey, oldContext)
    }
  }

  private def logRequestDetails(req: Request[_]) {
    import scala.collection.JavaConversions._

    info("  " + req.toString)

    info("  Request parameters:")
    for ((param, values) <- req.queryString if !(List("password", "body").contains(param.toLowerCase))) {
      info("    \"" + param + "\" -> " + values.mkString(", "))
    }

    info("  Session parameters:")
    for ((param, value) <- req.session.data) {
      info("    \"" + param + "\" -> " + value)
    }

    info("  Flash parameters:")
    info("    " + req.flash.toString)

    info("  Headers:")
    for ((headerName, headerValues) <- req.headers.toMap if headerName.toLowerCase != "authorization") {
      info("    \"" + headerName + "\" -> " + headerValues.mkString(", "))
    }
  }
}

