package services.http.forms.purchase

import com.google.inject.Inject
import services.Utils
import services.http.forms.{ReadsForm, FormChecks, Form}


class SelectProductForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory)
  extends Form[SelectProductForm.Validated]
{
  import SelectProductForm.{Params}

  //
  // Field values and validations
  //
  val product = field(Params.ProductId).validatedBy { paramValues =>
    check(paramValues).isProductId
  }

  //
  // Form[ValidatedSelectProductForm] members
  //
  protected def formAssumingValid: SelectProductForm.Validated = {
    // Safely access the account value in here
    SelectProductForm.Validated(product.value.get)
  }
}


object SelectProductForm {
  object Params {
    val ProductId = "order.productId"
  }

  /** Class to which the fully validated SelectProductForm resolves */
  case class Validated(product: Product)
}


class SelectProductFormFactory @Inject()(purchaseFormValidations: PurchaseFormChecksFactory)
  extends ReadsForm[SelectProductForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): SelectProductForm = {
    new SelectProductForm(readable, purchaseFormValidations)
  }

  //
  // ReadsForm[SelectProductForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): SelectProductForm = {
    apply(readable)
  }
}
