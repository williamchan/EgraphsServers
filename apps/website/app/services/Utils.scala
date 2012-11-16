package services

import play.api.mvc.Results.{Redirect}
import play.api.Play
import play.api.Play.current
import com.google.inject.Inject
import java.util
import org.squeryl.Query
import java.io._
import java.io.{Serializable, PrintWriter, StringWriter}
import play.api.mvc.Results.Redirect
import play.api.mvc.Result
import play.api.mvc.Request
import play.api.Play.current

/**
 * Helpful utilities with no other place to call home
 */
class Utils @Inject() {

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

  //TODO: PLAY20 migration. write test, this makes me sad to do. but everything is not compiling now, with 400+ compilation errors
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
    // source: Play 1.2.5 slugify. Stolen wholesale from https://github.com/playframework/play/blob/master/framework/src/play/templates/JavaExtensions.java#L364
    val noAccents = Normalizer.normalize(toSlugify, Normalizer.Form.NFKC).replaceAll("[àáâãäåāąă]", "a").replaceAll("[çćčĉċ]", "c").replaceAll("[ďđð]", "d").replaceAll("[èéêëēęěĕė]", "e").replaceAll("[ƒſ]", "f").replaceAll("[ĝğġģ]", "g").replaceAll("[ĥħ]", "h").replaceAll("[ìíîïīĩĭįı]", "i").replaceAll("[ĳĵ]", "j").replaceAll("[ķĸ]", "k").replaceAll("[łľĺļŀ]", "l").replaceAll("[ñńňņŉŋ]", "n").replaceAll("[òóôõöøōőŏœ]", "o").replaceAll("[Þþ]", "p").replaceAll("[ŕřŗ]", "r").replaceAll("[śšşŝș]", "s").replaceAll("[ťţŧț]", "t").replaceAll("[ùúûüūůűŭũų]", "u").replaceAll("[ŵ]", "w").replaceAll("[ýÿŷ]", "y").replaceAll("[žżź]", "z").replaceAll("[æ]", "ae").replaceAll("[ÀÁÂÃÄÅĀĄĂ]", "A").replaceAll("[ÇĆČĈĊ]", "C").replaceAll("[ĎĐÐ]", "D").replaceAll("[ÈÉÊËĒĘĚĔĖ]", "E").replaceAll("[ĜĞĠĢ]", "G").replaceAll("[ĤĦ]", "H").replaceAll("[ÌÍÎÏĪĨĬĮİ]", "I").replaceAll("[Ĵ]", "J").replaceAll("[Ķ]", "K").replaceAll("[ŁĽĹĻĿ]", "L").replaceAll("[ÑŃŇŅŊ]", "N").replaceAll("[ÒÓÔÕÖØŌŐŎ]", "O").replaceAll("[ŔŘŖ]", "R").replaceAll("[ŚŠŞŜȘ]", "S").replaceAll("[ÙÚÛÜŪŮŰŬŨŲ]", "U").replaceAll("[Ŵ]", "W").replaceAll("[ÝŶŸ]", "Y").replaceAll("[ŹŽŻ]", "Z").replaceAll("[ß]", "ss")
    val noApostrophes = noAccents.replaceAll("([a-z])'s([^a-z])", "$1s$2").replaceAll("[^\\w]", "-").replaceAll("-{2,}", "-")
    val noTrailingHyphens = noApostrophes.replaceAll("-+$", "").replaceAll("^-+", "")

    if (lowercaseOnly) noTrailingHyphens.toLowerCase else noTrailingHyphens
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
  @deprecated("this is balls", "don't use this balls")
  def asset(path: String): String = {
    controllers.routes.RemoteAssets.at(path).url
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
}

object Utils extends Utils {

  val defaultPageLength = 30

  def pagedQuery[A](select: Query[A], page: Int = 1, pageLength: Int = defaultPageLength, withTotal: Boolean = true): (Iterable[A], Int, Option[Int]) = {
    val total = if (withTotal) Some(select.count(_ => true)) else None
    val results = select.page(offset = pageLength * (page - 1), pageLength = pageLength)
    (results, page, total)
  }

  def logException(e: Throwable) {
    val stringWriter = new StringWriter()
    e.printStackTrace(new PrintWriter(stringWriter))
    play.api.Logger.error("Fatal error: " + e.getClass + ": " + e.getMessage)
    stringWriter.toString.split("\n").foreach(line => play.api.Logger.info(line))
  }
}
