package services.http.forms.purchase

import services.http.forms._
import models.enums.PrintingOption

class ReviewForm(val paramsMap: Form.Readable, check: FormChecks) extends Form[ReviewForm.Valid] {
  import ReviewForm.Params

  val validations = new ReviewForm.Validations(check)

  val highQualityPrint = new RequiredField[PrintingOption](Params.HighQualityPrint) {
    def validateIfPresent = {
      validations.validatePrintField(this.stringToValidate)
    }
  }

  override def formAssumingValid:ReviewForm.Valid = {
    new ReviewForm.Valid
  }
}

object ReviewForm {
  object Params {
    val HighQualityPrint = "order.review.highQualityPrint"
  }

  class Valid

  class Validations(check: FormChecks) {
    def validatePrintField(toValidate: String): Either[FormError, PrintingOption] = {
      for (printingChoice <- check.isSomeValue(
                               PrintingOption(toValidate),
                               "Was not a valid printing option"
                             ).right)
      yield {
        printingChoice
      }
    }
  }
}