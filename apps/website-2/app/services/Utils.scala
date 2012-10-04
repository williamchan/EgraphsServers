package services

import http.PlayConfig
import play.api.mvc.Results.{Redirect}
import play.api.Play
import play.api.Play.current
import com.google.inject.Inject
import java.util
import org.squeryl.Query
import java.io._
import scala.Some
import java.io.{Serializable, PrintWriter, StringWriter}
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.Play.current
import services.http.PlayConfigurationProperties

/**
 * Helpful utilities with no other place to call home
 */
class Utils @Inject()(@PlayConfig() playConfig: util.Properties) {

  /**
   * This implicit conversion converts a String option to a RichStringOption
   */
  implicit def stringOptionToRichStringOption(maybe: Option[String]): RichStringOption = RichStringOption(maybe) 

  case class RichStringOption(maybe: Option[String]) {
    /**
     * Instead of the String.toBoolean throwing an exception if the lower-case value of
     * the string is not exactly "true" or "false", this will be "true" only if there is a
     * string value with a lower-case value of exactly "true" and false otherwise.
     */
    lazy val toBoolean: Boolean = { 
      maybe match {
        case Some(stringValue) => java.lang.Boolean.valueOf(stringValue)
        case None => false
      }
    }
  }

  //TODO: write test, this makes me sad to do. but everything is not compiling now, with 400+ compilation errors
  /**
   * @return The first value in the map if there is one, or returns the elseValue.
   */
  def getFromMapFirstInSeqOrElse[T](key: String, elseValue: T, map: Map[String, Seq[T]]): T  = {
    map.get("secretKey").getOrElse(Seq(elseValue)).headOption.getOrElse(elseValue)
  }

  /**
   * @return The optionally the first value in a sequence if there is one. 
   */
  def getOptionFirstInSeq[T](optionSeq: Option[Seq[T]]): Option[T]  = {
    optionSeq match {
      case None => None
      case Some(seq) => 
        if(seq.isEmpty) {
          None
        } else {
          Some(seq.head)
        }
    }
  }

  // TODO: PLAY20 migration. Document
  def slugify(toSlugify: String, lowercaseOnly: Boolean = true): String = {
    import java.text.Normalizer
    // source: https://github.com/julienrf/chooze/blob/master/app/util/Util.scala#L6
    val slug = Normalizer.normalize(toSlugify, Normalizer.Form.NFD).replaceAll("[^\\w ]", "").replace(" ", "-").toLowerCase

    if (lowercaseOnly) {
      slug.toLowerCase()
    } else {
      slug
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

  def toOption(str: String): Option[String] = {
    str match {
      case null => None
      case s if (s.isEmpty) => None
      case _ => Some(str)
    }
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
  def closing[T <: {def close() : Any}, U](resource: T)(usesResource: T => U): U = {
    try {
      usesResource(resource)
    }
    finally {
      resource.close()
    }
  }

  /**
   * Redirects to a targetUrl found in the request params. If that url could not be found, it redirects to
   * the URL in the argument.
   */
  def redirectToClientProvidedTarget(urlIfNoTarget: String)(implicit request: Request[_]): Result = {
    import services.http.SafePlayParams.Conversions._
    import play.api.data._
    import play.api.data.Forms._
    
    val targetUrlForm = Form(single("targetUrl" -> text))
    val redirectUrl = targetUrlForm.fold(noTargetUrl => urlIfNoTarget, targetUrl => targetUrl)
    
    Redirect(redirectUrl)
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

  /**
   * Returns valid file asset relative URLs on the Play application's path
   * given a putative one.
   *
   * Throws an exception if the path could not be resolved to an actual file.
   * For example, Utils.asset("public/javascripts/jquery1.5.2") may succeed,
   * whereas any misspelling thereof would fail.
   * @param path the path to check for validity
   *
   * @return a valid path, or throw an exception
   */
  def asset(path: String): String = {
    controllers.routes.Assets.at(path).url
  }

  implicit def properties(pairs: (AnyRef, AnyRef)*): util.Properties = {
    val props = new util.Properties

    for (pair <- pairs) props.put(pair._1, pair._2)

    props
  }

  /**
   * @param bytes bytes to be written to the file
   * @param file the intended file location
   */
  def saveToFile(bytes: Array[Byte], file: File) {
    file.getParentFile.mkdirs()
    val out = new FileOutputStream(file)
    out.write(bytes)
    out.close()
  }

  /**
   * Why am I writing this? Because the application.baseUrl config parameter seems to have a trailing "/" in all of the
   * Play examples while a url from ActionDefinitions has a leading "/". So, I wanted to make sure that we are using
   * application.baseUrl correctly and also correctly composing absolute Urls.
   *
   * I am hopeful these URL-related helper methods will go away once we move to Play 2.0.
   *
   * @param baseUrl eg "https://www.egraphs.com/"
   * @param relativeUrl eg "/login"
   * @return the concatenated full Url
   */
  private[services] def composeUrl(baseUrl: String, relativeUrl: String): String = {
    (baseUrl.endsWith("/"), relativeUrl.startsWith("/")) match {
      case (true, true) => baseUrl + relativeUrl.substring(1)
      case (true, false) | (false, true) => baseUrl + relativeUrl
      case (false, false) => baseUrl + "/" + relativeUrl
    }
  }
}

object Utils extends Utils(PlayConfigurationProperties.properties) {

  /**DIY exhaustiveness-checking enum type. See https://gist.github.com/1057513 */
  trait Enum {

    import java.util.concurrent.atomic.AtomicReference

    type EnumVal <: Value //This is a type that needs to be found in the implementing class

    private val _values = new AtomicReference(Vector[EnumVal]()) //Stores our enum values. Using AtomicReference due to Concurrency paranoia

    //Adds an EnumVal to our storage, uses CCAS to make sure it's thread safe, returns the ordinal
    private final def addEnumVal(newVal: EnumVal): Int = {
      import _values.{get, compareAndSet => CAS}
      require(this(newVal.name) == None)

      val oldVec = _values.get
      val newVec = oldVec :+ newVal
      if ((get eq oldVec) && CAS(oldVec, newVec)) newVec.indexWhere(_ eq newVal) else addEnumVal(newVal)
    }

    def values: Vector[EnumVal] = _values.get //Here you can get all the enums that exist for this type
    /**
     * Returns an Option[EnumVal] if the Enum has a corresponding mapping form string to enumval
     * @param name String name of enum
     * @return
     */
    def apply(name: String): Option[EnumVal] = {
      values.filter(en => en.name == name).headOption
    }

    //This is the trait that we need to extend our EnumVal type with, it does the book-keeping for us
    protected trait Value extends Serializable {
      //Enforce that no one mixes in Value in a non-EnumVal type
      self: EnumVal =>
      final val ordinal = addEnumVal(this) //Adds the EnumVal and returns the ordinal

      def name: String //All enum values should have a name

      //And that name is used for the toString operation
      override def toString = name

      override def equals(other: Any) = {
        other match {
          case thisType: Value =>
            thisType.name == this.name

          case otherType =>
            this eq other.asInstanceOf[AnyRef]
        }
      }

      override def hashCode = 31 * (this.getClass.## + name.## + ordinal)
    }

  }

  val defaultPageLength = 30

  def pagedQuery[A](select: Query[A], page: Int = 1, pageLength: Int = defaultPageLength, withTotal: Boolean = true): (Iterable[A], Int, Option[Int]) = {
    val total = if (withTotal) Some(select.count(_ => true)) else None
    val results = select.page(offset = pageLength * (page - 1), pageLength = pageLength)
    (results, page, total)
  }

  def logException(e: Throwable) {
    val stringWriter = new StringWriter()
    e.printStackTrace(new PrintWriter(stringWriter))
    play.Logger.error("Fatal error: " + e.getClass + ": " + e.getMessage)
    stringWriter.toString.split("\n").foreach(line => play.Logger.info(line))
  }
}
