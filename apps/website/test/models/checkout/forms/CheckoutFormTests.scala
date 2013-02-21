package models.checkout.forms

import utils.{TestData, DBTransactionPerTest, DateShouldMatchers, EgraphsUnitTest}
import play.api.test.FakeRequest
import models.checkout.{LineItemTestData, CouponLineItemType, EgraphOrderLineItemType}
import play.api.data.Form
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher, Matcher, MatchResult}
import play.api.data.FormError
import scala.Some


class CheckoutFormTests extends EgraphsUnitTest with DateShouldMatchers with DBTransactionPerTest {

  "EmailForm" should "bind to an AccountEmail" in {
    val email = "jj420@heyhey.biz"
    val boundForm = bindToForm(EmailForm)(EmailForm.FormKeys.emailKey -> email)

    boundForm should not have errors
    boundForm should haveValue(email)
  }

  "EgraphForm" should "bind to an EgraphOrderLineItemType" in {
    val (boundForm, expectedValue) = boundEgraphFormAndExpectedValue()

    boundForm should not have errors
    boundForm should haveValue(expectedValue)
  }

  it should "fail to bind with an invalid product id" in {
    val (boundForm, _) = boundEgraphFormAndExpectedValue(
      EgraphForm.FormKeys.productId -> "0"
    )

    boundForm should have (errors)
    boundForm should haveApiErrors(ApiError.InvalidProduct)
  }

  "CouponForm" should "bind to a CouponLineItemType" in {
    val coupon = TestData.newSavedCoupon()
    val boundForm = bindToForm(CouponForm)(CouponForm.FormKeys.couponCode -> coupon.code)

    boundForm should not have errors
    boundForm.value match {
      case Some(CouponLineItemType(_, coupon, _)) =>
      case _ => fail(boundForm.value + "did not have coupon: " + coupon)
    }
  }

  "PaymentForm" should "bind to a CashTransactionLineItemType" in {
    import PaymentForm.FormKeys._
    val expectedValue = LineItemTestData.randomCashTransactionType
    val boundForm = bindToForm(PaymentForm)(
      stripeToken -> expectedValue.stripeCardTokenId.get,
      postalCode -> expectedValue.billingPostalCode.get
    )

    boundForm should not have errors
    boundForm should haveValue(expectedValue)
  }

  "ShippingAddressForm" should "bind to a ShippingAddress" in {
    import ShippingAddressForm.FormKeys._
    import FakeFormData._
    val address = randomShippingAddress
    val boundForm = bindToForm(ShippingAddressForm)(
      nameKey -> address.name,
      addressLine1Key -> address.addressLine1,
      cityKey -> address.city,
      stateKey -> address.state,
      postalCodeKey -> address.postalCode
    )

    boundForm should not have errors
    boundForm should haveValue(address)
  }


  //
  // Helpers
  //
  private def bindToForm[T](form: CheckoutForm[T])(body: (String, String)*) = {
    implicit val request = FakeRequest().withFormUrlEncodedBody(body: _*)
    form.bindFromRequest()
  }

  private def boundEgraphFormAndExpectedValue(_body: (String, String)*) = {
    import EgraphForm.FormKeys._
    val defaultBody = Map (
      productId -> TestData.newSavedProduct().id.toString,
      recipientName -> TestData.generateFullname(),
      isGift -> "false",
      desiredText -> TestData.random.nextString(10),
      messageToCeleb -> TestData.random.nextString(16),
      framedPrint -> "false"
    )

    val _bodyMap = _body.toMap[String, String]
    val body = defaultBody -- _bodyMap.keysIterator ++ _bodyMap
    val boundForm = bindToForm(EgraphForm)(body.toSeq: _*)
    val expectedValue = EgraphOrderLineItemType(
      body(productId).toLong,
      body(recipientName),
      body(isGift).toBoolean,
      Some(body(desiredText)),
      Some(body(messageToCeleb)),
      body(framedPrint).toBoolean
    )
    (boundForm, expectedValue)
  }


  //
  // Matchers
  //
  private def haveApiError(error: ApiError) = Matcher { left: Seq[FormError] =>
    MatchResult(
      left flatMap (formError => ApiError(formError.message)) contains error,
      "Errors did not contain " + error.name,
      "Errors contained" + error.name
    )
  }

  private val errors = new HavePropertyMatcher[Form[_], Boolean] {
    def apply(form: Form[_]) = HavePropertyMatchResult[Boolean] (
      matches = !form.errors.isEmpty, 
      propertyName = "errors.isEmpty", 
      expectedValue = false, 
      actualValue = form.errors.isEmpty
    )
  }

  def haveApiErrors(errors: ApiError*) = (have (apiErrors(errors: _*)))
  private def apiErrors(errors: ApiError*) = new HavePropertyMatcher[Form[_], Iterable[ApiError]] {
    def apply(form: Form[_]) = {
      val formApiErrors = form.errors flatMap { formError => ApiError(formError.message) }
      
      HavePropertyMatchResult[Iterable[ApiError]] (
        errors forall { formApiErrors contains _ },
        "errors",
        errors,
        formApiErrors
      )
    }
  }

  def haveValue[T](expected: T) = (have (value(expected)))
  private def value[T](expectedValue: T) = new HavePropertyMatcher[Form[T], Option[T]] {
    def apply(form: Form[T]) = HavePropertyMatchResult[Option[T]] (
      form.value == Some(expectedValue),
      "value",
      Some(expectedValue),
      form.value
    )
  }
}

object FakeFormData {
  import TestData._

  def randomShippingAddress = ShippingAddress(
    name = TestData.generateFullname(),
    addressLine1 = random.nextString(10),
    addressLine2 = None,
    city = random.nextString(8),
    state = random.nextString(2),
    postalCode = random.nextString(5)
  )
}