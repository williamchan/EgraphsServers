package services.http.forms.purchase

import com.google.inject.Inject
import services.Utils
import services.http.forms.{ReadsForm, FormChecks, Form}


class SelectProductForm(val paramsMap: Form.Readable, check: FormChecks)
  extends Form[SelectProductForm.Validated]
{
  import SelectProductForm.{Fields}

  //
  // Field values and validations
  //
  val product = new RequiredField[models.Product](Fields.ProductId.name) {
    def validateIfPresent = {
      for (idAsLong <- check.isLong(stringToValidate).right;
           product <- check.isProductId(idAsLong).right)
      yield product
    }
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
  object Fields extends Utils.Enum {
    sealed case class EnumVal(name: String) extends Value

    val ProductId = EnumVal("order.productId")
  }

  /** Class to which the fully validated SelectProductForm resolves */
  case class Validated(product: Product)
}


class SelectProductFormFactory @Inject()(formChecks: FormChecks)
  extends ReadsForm[SelectProductForm]
{
  //
  // Public members
  //
  def apply(readable: Form.Readable): SelectProductForm = {
    new SelectProductForm(readable, formChecks)
  }

  //
  // ReadsForm[SelectProductForm] members
  //
  def instantiateAgainstReadable(readable: Form.Readable): SelectProductForm = {
    apply(readable)
  }
}
