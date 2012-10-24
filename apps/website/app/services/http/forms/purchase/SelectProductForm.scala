package services.http.forms.purchase

import services.http.forms.Form

/**
 * Purchase flow form for selecting a product.
 */
class SelectProductForm(val paramsMap: Form.Readable, check: PurchaseFormChecksFactory)
  extends Form[Product]
{
  import SelectProductForm.Params

  //
  // Field values and validations
  //
  val product = field(Params.ProductId).validatedBy { paramValues =>
    check(paramValues).isProductId
  }

  //
  // Form members
  //
  protected def formAssumingValid: Product = {
    product.value.get
  }
}

object SelectProductForm {
  object Params {
    val ProductId = "order.productId"
  }
}
