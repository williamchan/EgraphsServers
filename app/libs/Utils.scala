package libs

import play.mvc.Router
import play.Play


/**
 * Helpful utilities with no other place to call home
 */
object Utils {

  /**
   * Creates an Option out of any nullable type.
   *
   * @param toMakeOption the value to make an Option
   *
   * @return the Option of toMakeOption. This is None if toMakeOption was null,
   *   or if it was an empty String. Otherwise it's Some(toMakeOption)
   */
  def optional[T <: AnyRef](toMakeOption: T): Option[T] = {
    toMakeOption match {
      case null => None
      case aString: String if aString == "" => None
      case value => Some(value)
    }
  }

  /**
   * Turns any Iterable into a map keyed by a provided function.
   *
   * @param toMap the Iterable to turn into a map
   * @param key function that produces a key for each instance in toMap
   *
   * @return the desired map
   */
  def toMap[K, V](toMap: Iterable[V], key: (V) => K): Map[K, V] = {
    toMap.foldLeft(Map.empty[K, V])((growingMap, next) => growingMap + (key(next) -> next))
  }

  /**
   * Manages closing behavior for functions that use resources that require closing afterwards.
   *
   * Best to use with stateful objects like PrintWriters, InputStreams, Connections, etc.
   * Example:
   * {{{
   * closing(new FileOutputStream(myFile)) { stream =>
   *   // Do things with the stream here...don't bother closing it.
   * }
   * // The stream is closed at this point
   * }}}
   *
   * @param resource the resource to close. It must have a no-args close method.
   * @param usesResource function that performs actions using the resource.
   *
   * @return the result of usesResource, after closing the resource.
   */
  def closing [T <: { def close(): Any }, U](resource: T)(usesResource: T => U): U = {
    try {
      usesResource(resource)
    }
    finally {
      resource.close()
    }
  }

  /**
   * Returns up the ActionDefinition for a controller method with a parameter map.
   *
   * For example, to redirect to Shaq's celebrity page:
   * {{{
   *   val actionDef = Utils.lookupUrl(
   *     "controllers.CelebrityController.index",
   *     Map("celebrityUrlSlug" -> "Shaq")
   *   )
   *   redirect(actionDef.url)
   * }}}
   */
  def lookupUrl(controllerMethod: String, params: Map[String, AnyRef]=Map()): Router.ActionDefinition = {
    import scala.collection.JavaConversions._

    if (params == Map.empty) {
      Router.reverse(controllerMethod)
    } else {
      Router.reverse(controllerMethod, params: Map[String, Object])
    }
  }

  /**
   * Returns a configuration property that must exist in application.conf.
   *
   * Throws a runtime exception with a reasonable error message if it didn't exist.
   */
  def requiredConfigurationProperty(property: String): String = {
    val theValue = Play.configuration.getProperty(property)
    require(
      theValue != null,
      "Property \"" + property + "\" in application.conf was required but not present."
    )

    theValue
  }
}