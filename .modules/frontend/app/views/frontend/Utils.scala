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
   *   <form "method" -> "GET">
   * }}}
   */
  def formatAttributeTuplesToHtml(tuples: (String, String)*): String = {
    val attributeStrings = for (val (name, value) <- tuples) yield {
      "\"" + name + "\" = \"" + value + "\""
    }

    attributeStrings.mkString(" ")
  }
}
