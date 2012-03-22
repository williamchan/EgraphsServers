package services

import utils.EgraphsUnitTest
import org.stringtemplate.v4.STGroup

class TemplateEngineTests extends EgraphsUnitTest {
  
  "TemplateEngine" should "return the same string given an empty template" in {
    new TemplateEngine().evaluate("Herp", Map.empty) should be ("Herp")
  }

  it should "properly templatize a value" in {
    new TemplateEngine().evaluate("Herp {value}", Map("value" -> "derp")) should be ("Herp derp")
  }

  it should "escape back quotes correctly" in {
    new TemplateEngine().evaluate("Herp \\{value}", Map("valor" -> "nada")) should be ("Herp {value}")
  }
  
  it should "tolerate when the map contains more values than there are" in {
    new TemplateEngine().evaluate("Herp derp", Map("value" -> "lonely")) should be ("Herp derp")
  }

  it should "support internal methods" in {
    /*
    val template = """
    hello(name) ::= "Herp hello, {name} derp"

    story(celebName) ::= "Derp {hello(celebName)}"
    """*/
    val group = new STGroup('{', '}')
    group.defineTemplate("helloworld", "name", "Herp hello, {name}")
    group.defineTemplate("story", "celebName", "Derp {helloworld(\"herpy derpson\")}")
    // val group = new org.stringtemplate.v4.STGroupString("derp", template, '{', '}')

    val story = group.getInstanceOf("story")
    story.add("celebName", "John Cleese")

    println(story.render())
  }
}
