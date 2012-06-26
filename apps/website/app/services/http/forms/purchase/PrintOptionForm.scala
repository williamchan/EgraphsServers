package services.http.forms.purchase

import services.http.forms._
import models.enums.PrintingOption

/**
 * Purchase flow form for the review page: this is where you specify
 * whether you want a print or not.
 */
class PrintOptionForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory) extends Form[PrintingOption] {
  import PrintOptionForm.Params

  val highQualityPrint = field(Params.HighQualityPrint).validatedBy { paramValues =>
    check(paramValues).isPrintingOption
  }

  //
  // Form members
  //
  override def formAssumingValid:PrintingOption= {
    highQualityPrint.value.get
  }
}

object PrintOptionForm {
  object Params {
    val HighQualityPrint = "order.printOption"
  }

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