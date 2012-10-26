package services.http.forms.purchase

import com.google.inject.Inject
import services.http.forms._
import controllers.website.consumer.PostBulkEmailController

/**
 * Provides readers for most of the HTTP form types we use.
 *
 * @param formChecks low-level checks used for validating forms
 * @param purchaseFormChecksFactory high-level checks used for
 *     validating purchase forms
 **/
class FormReaders @Inject()(
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

  def forCustomerLoginForm: ReadsForm[CustomerLoginForm] = {
    newReaderWithConstructor { readable =>
      new CustomerLoginForm(readable, formChecks)
    }
  }

  def forRegistrationForm: ReadsForm[AccountRegistrationForm] = {
    newReaderWithConstructor { readable =>
      new AccountRegistrationForm(readable, formChecks)
    }
  }

  def forEmailSubscriptionForm: ReadsForm[EmailSubscriptionForm] = {
    newReaderWithConstructor { readable =>
      new EmailSubscriptionForm(readable, formChecks)
    }
  }

  /**
   * @param shippingFormOption Provide a shipping form only if it should be
   *   used to grab information that wasn't provided to the billing form. e.g.
   *   in the case when the customer specified "My billing info is my shipping info"
   */
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

  //
  // Private members
  //
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
