package services

import org.stringtemplate.v4.ST

/**
 * Performs templating based on the java [[http://www.stringtemplate.org/ StringTemplate]]
 * library. Template variables are delimted by opened and closed curly braces.
 */
class TemplateEngine {
  /**
   * Evaluates the template `templateText` with value mappings located in `templateMappings`.
   *
   * Usage:
   * {{{
   *   val engine = AppConfig.instance[TemplateEngine]
   *
   *   // Prints 'Herp derp'
   *   println(engine.evaluate("Herp {value}", Map("value" -> "derp"))
   * }}}
   *
   *
   * @param templateText the template to evaluate, with variables marked by angle brackets
   * @param templateMappings the variable values to substitute in templateText
   * @return the evaluated template
   */
  def evaluate(templateText: String, templateMappings: Map[String, String]): String = {
    val template = new ST(templateText, '{', '}')

    for ((name, value) <- templateMappings) template.add(name, value)

    template.render()
  }
}
