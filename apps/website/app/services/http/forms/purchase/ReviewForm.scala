package services.http.forms.purchase

import services.http.forms._
import models.enums.PrintingOption

class ReviewForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory) extends Form[ReviewForm.Valid] {
  import ReviewForm.Params

  val highQualityPrint = field(Params.HighQualityPrint).validatedBy { paramValues =>
    check(paramValues).isPrintingOption
  }

  //
  // Form members
  //
  override def formAssumingValid:ReviewForm.Valid = {
    new ReviewForm.Valid(highQualityPrint.value.get)
  }
}

object ReviewForm {
  object Params {
    val HighQualityPrint = "order.review.highQualityPrint"
  }

  class Valid(printingChoice: PrintingOption)

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