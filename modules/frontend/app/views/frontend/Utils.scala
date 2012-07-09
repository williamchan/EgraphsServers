package views.frontend

/**
 * Front-end utilities that otherwise had no home.
 */
object Utils {
  
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

  def feedDialogLink(appId: String,
                     picUrl:String,
                     name:String,
                     caption:String,
                     description:String,
                     link:String) : String = {

    "https://www.facebook.com/dialog/feed?" +
      "app_id=" + appId +
      "&redirect_uri=" + link +
      "&picture=" + picUrl +
      "&name=" + name +
      "&caption=" + caption +
      "&description=" + description
  }
}
