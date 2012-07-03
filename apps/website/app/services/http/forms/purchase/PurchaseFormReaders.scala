package services.http.forms.purchase

import com.google.inject.Inject
import services.http.forms.{Form, ReadsForm, FormChecks}

class PurchaseFormReaders @Inject()(
  formChecks: FormChecks,
  purchaseFormChecksFactory: PurchaseFormChecksFactory
) {

  def forPersonalizeForm: ReadsForm[PersonalizeForm] = {
    newReaderWithConstructor { readable =>
      new PersonalizeForm(readable, formChecks, purchaseFormChecksFactory)
    }
  }

  def forShippingForm: ReadsForm[CheckoutShippingForm] = {
    newReaderWithConstructor { readable =>
      new CheckoutShippingForm(readable, formChecks, purchaseFormChecksFactory)
    }
  }

  def forBillingForm(shippingFormOption: Option[CheckoutShippingForm])
  : ReadsForm[CheckoutBillingForm] = {
    newReaderWithConstructor { readable =>
      new CheckoutBillingForm(
        readable,
        formChecks,
        purchaseFormChecksFactory,
        shippingFormOption
      )
    }
  }

  private def newReaderWithConstructor[FormType <: Form[_] : Manifest]
  (constructFormFromReadable: Form.Readable => FormType): ReadsForm[FormType] =
  {
    new ReadsForm[FormType]() {
      def instantiateAgainstReadable(readable: Form.Readable): FormType = {
        constructFormFromReadable(readable)
      }
    }
  }
}
