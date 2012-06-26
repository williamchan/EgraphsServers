package services.http.forms.purchase

import services.http.forms.{Form}


class SelectProductForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory)
  extends Form[Product]
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
  protected def formAssumingValid: Product = {
    // Safely access the account value in here
    product.value.get
  }
}

object SelectProductForm {
  object Params {
    val ProductId = "order.productId"
  }
}
