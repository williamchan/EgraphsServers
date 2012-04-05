package services

import http.PlayConfig
import play.mvc.Router
import play.Play
import com.google.inject.Inject
import java.util.Properties
import org.squeryl.Query

/**
 * Helpful utilities with no other place to call home
 */
class Utils @Inject()(@PlayConfig() playConfig: Properties)
{
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

  def toOption(str: String): Option[String] = {
    if (str.isEmpty) None else Some(str)
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
   *     "controllers.WebsiteControllers.getCelebrity",
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
   * Throws an IllegalArgumentException with a reasonable error message if it didn't exist.
   */
  def requiredConfigurationProperty(property: String): String = {
    val theValue = playConfig.getProperty(property)
    if (theValue == null) {
      val errorMessage = "Property \"" + property +
        "\" in application.conf was required but not present."

      play.Logger.error(errorMessage)
      throw new IllegalArgumentException(errorMessage)
    }
    else {
      theValue
    }

    theValue
  }

  /**
   * Turns a set of (String -> Option) tuples into a map containing only the fields
   * whose Options contained some value. For example,
   * {{{
   * val sparseMap = Map("one" -> Some(1), "two" -> None)
   *
   * sparseMap == Map("one" -> Some(1)) // True
   * }}}
   */
  def makeOptionalFieldMap(optionalFields: List[(String, Option[Any])]): Map[String, Any] = {
    optionalFields.foldLeft(Map.empty[String, Any])((growingMap, nextField) =>
      nextField._2 match {
        case None => growingMap
        case Some(value) => growingMap + (nextField._1 -> value)
      }
    )
  }

  implicit def properties(pairs: (AnyRef, AnyRef)*): Properties = {
    val props = new Properties
    
    for (pair <- pairs) props.put(pair._1, pair._2)

    props
  }
}

object Utils extends Utils(Play.configuration) {
  /** DIY exhaustiveness-checking enum type. See https://gist.github.com/1057513 */
  trait Enum {
    import java.util.concurrent.atomic.AtomicReference //Concurrency paranoia

    type EnumVal <: Value //This is a type that needs to be found in the implementing class

    private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values

    //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
    private final def addEnumVal(newVal: EnumVal): Int = { import _values.{get, compareAndSet => CAS}
      val oldVec = get
      val newVec = oldVec :+ newVal
      if((get eq oldVec) && CAS(oldVec, newVec)) newVec.indexWhere(_ eq newVal) else addEnumVal(newVal)
    }

    def values: Vector[EnumVal] = _values.get //Here you can get all the enums that exist for this type

    //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
    protected trait Value { self: EnumVal => //Enforce that no one mixes in Value in a non-EnumVal type
      final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

      def name: String //All enum values should have a name

      override def toString = name //And that name is used for the toString operation
      override def equals(other: Any) = this eq other.asInstanceOf[AnyRef]
      override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
    }
  }

  val defaultPageLength = 30

  def pagedQuery[A](select: Query[A], page: Int = 1, pageLength: Int = defaultPageLength, withTotal: Boolean = true): (Iterable[A], Int, Option[Int]) = {
    val total = if (withTotal) Some(select.count((a) => true)) else None
    val results = select.page(offset = pageLength * (page - 1), pageLength = pageLength)
    (results, page, total)
  }
}