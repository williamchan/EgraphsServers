package views.frontend.tags.ng

/**
 *  Builds a string angular binding
 *
**/

object Utils {
  def binding(model: String, id: String) : String = {
    "{{" + model + id + "}}"
  }
}
