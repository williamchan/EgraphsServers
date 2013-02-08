package egraphs.playutils

import play.api.data.Form
import play.api.mvc.Request
import play.api.libs.json.Json
import play.api.data.FormError
import play.api.mvc.Flash
import play.api.mvc.SimpleResult

/**
 * When you want to use the data from a bad post after a redirect you would have needed to added
 * the form data to the flash to populate that page.  Now all you have to do its import the implicit
 * conversions:
 *
 *   import egraphs.playutils.FlashableForm._
 *
 * and then when redirection:
 *
 *   Redirect(url).flashingFormData(theBadForm)
 *
 * and in the controller for the url above just pass in the form to the template, assuming you are
 * using FieldConstructors in your template.
 *
 *   Ok(views.html.index("other arguments", form.bindWithFlashData))
 *   
 * USER WARNING: If there are two or more forms with fields with the same name that were stored in
 *  the flash this will not work as intended.  Make sure form field names are unique on a page.
 *
 */
case class FlashableForm[A](form: Form[A]) {
  import FlashableForm.CONTAINS_FLASHED_FORM

  def bindWithFlashData(implicit request: Request[_]): Form[A] = {
    val flashData = Flash.serialize(request.flash)
    if (flashData.contains(CONTAINS_FLASHED_FORM) && java.lang.Boolean.valueOf(flashData(CONTAINS_FLASHED_FORM))) {
      val boundForm = form.bind(flashData)
      boundForm
    } else {
      form
    }
  }
}

case class SimpleResultWithFlashedForm(result: SimpleResult[_]) {
  import FlashableForm.CONTAINS_FLASHED_FORM

  /**
   * USER WARNING: If there are two or more forms with fields with the same name that were stored in
   * the flash this will not work as intended.  Make sure form field names are unique on a page.
   */
  def flashingFormData(form: Form[_]) = {
    val flash = form.data + (CONTAINS_FLASHED_FORM -> true.toString)
    result.flashing(flash.toSeq: _*)
  }
}

object FlashableForm {
  val CONTAINS_FLASHED_FORM = "contains-form-data"
  implicit def form2FlashableForm[A](form: Form[A]) = FlashableForm(form)
  implicit def result2SimpleResultWithFlashedForm(result: SimpleResult[_]) = SimpleResultWithFlashedForm(result)
}