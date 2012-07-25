package views.frontend

/**
 * Front-end utilities that otherwise had no home.
 */
object Utils {
  val cdnAssets = play.configuration.conf.getProperty("cdn.enabled")
  val cdnUrl = play.configuration.conf.getProperty("cloudfront.domain")
  /**
   * Formats a series of String / String tuples into an attribute string suitable for
   * use in an HTML tag. This is most useful when writing a wrapper around the creation
   * of HTML tags, for example if making a @safeForm tag that always provides an authenticity
   * token.
   * 
   * For example, if used in a template:
   * {{{
   *   <form @formatAttributeTuplesToHtml("method" -> "GET")>
   *   
   *   // renders as
   *   <form method="GET">
   * }}}
   */
  def formatAttributeTuplesToHtml(tuples: (String, String)*): String = {
    val attributeStrings = for (val (name, value) <- tuples) yield {
      name + " = \"" + value + "\""
    }

    attributeStrings.mkString(" ")
  }

  /**
   * Drop-in replacement for @asset. Use to take advantage of cloudfront on live.
   * Paths are always absolute to root. Leading '/' is optional.
   * @param path
   * @return path to asset.
   */

  def cdnAsset(path: String) : String = {
    cdnAssets match {
      case "true" =>
        path(0) match {
          case '/' => "https://" + cdnUrl + path
          case _ =>  "https://" + cdnUrl + "/" + path
        }
      case _ => play.mvc.Router.reverse(play.Play.getVirtualFile(path))
    }
  }

  /**
   *  Returns a string for an angular.js binding
   *
   **/
  def binding(model: String, id: String) : String = {
    "{{" + model + id + "}}"
  }

  def heOrShe(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "he"
      case (true, true) => "He"
      case (false, false) => "she"
      case (false, true) => "She"
    }
  }

  def himOrHer(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "him"
      case (true, true) => "Him"
      case (false, false) => "her"
      case (false, true) => "Her"
    }
  }

  def hisOrHer(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "his"
      case (true, true) => "His"
      case (false, false) => "her"
      case (false, true) => "Her"
    }
  }

  def getFacebookShareLink(appId: String,
                           picUrl: String,
                           name: String,
                           caption: String,
                           description: String,
                           link: String): String = {

    "https://www.facebook.com/dialog/feed?" +
      "app_id=" + appId +
      "&redirect_uri=" + link +
      "&picture=" + picUrl +
      "&name=" + name +
      "&caption=" + caption +
      "&description=" + description +
      "&link=" + link
  }

  def getTwitterShareLink(link: String,
                          text: String): String = {
    "https://twitter.com/intent/tweet?" +
      "url=" + link +
      "&text=" + text
  }
}
