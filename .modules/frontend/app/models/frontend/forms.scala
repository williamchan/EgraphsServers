//
// Classes that relate to form processing. Actual forms from the back-end get transformed
// into these types for rendering. (See main application services.http.forms and services.mvc)
//

package models.frontend.forms

import play.templates.Html

/**
 * An error in the form that was identified by the server.
 */
case class FormError(description: String) {
  override def toString: String = {
    description
  }
}


/**
 * A form field of a particular type. It encompasses correct and error states
 * of any field in a form.
 *
 * You can iterate over it to get its value, or just directly use its value
 * property.
 *
 * Usage:
 * {{{
 *   // fieldtest.scala.html
 *   @(nameField: models.frontend.forms.Field)
 *
 *   <h1>Name Reporter</h1>
 *   <h2>My names:</h2>
 *
 *   <!-- Print out all the names in a list if there were any -->
 *   @if(nameField.nonEmpty) {
 *     <ol>
 *       @for(value <- nameField) {
 *         <li>@value</li>
 *       }
 *     </ol>
 *   } else {
 *     <div>I have no name</div>
 *   }
 *
 *   <!-- Print out all the errors -->
 *   @nameField.ifError {
 *     <div class="error">@nameField.error</div>
 *   }
 *
 * }}}
 *
 * @param name the name of the field, e.g. "Email Address". It should be
 *    compatible with the "name" field of an HTML input tag
 * @param values the 0 or more values the field should contain.
 * @param error any error associated with the form (e.g. in case of redirect)
 */
case class Field[+ValueType](
  name: String,
  values: Iterable[ValueType]=None,
  error: Option[FormError]=None
) extends Iterable[ValueType] {
  final def value = {
    values.headOption
  }

  /**
   * Conditionally executes `htmlGenerator` when an error is present.
   *
   * Usage:
   * {{{
   *   <div class="my-form-field @myField.ifError {my-error-class}">...</div>
   * }}}
   *
   * @param htmlGenerator HTML to present in case there was an error.
   *
   * @return Either the HTML returned by `htmlGenerator` or an empty String.
   */
  def ifError(htmlGenerator: => Html): Html = {
    error.map(error => htmlGenerator).getOrElse(Html.empty)
  }

  /**
   * Conditionally executes `htmlGenerator` when there is no error present
   *
   * See ifError for usage
   *
   * @param htmlGenerator
   * @return
   */
  def ifNotError(htmlGenerator: => Html): Html = {
    error.map(error => Html.empty).getOrElse(htmlGenerator)
  }

  //
  // Iterable[ValueType] members
  //
  def iterator: Iterator[ValueType] = {
    values.iterator
  }
}
