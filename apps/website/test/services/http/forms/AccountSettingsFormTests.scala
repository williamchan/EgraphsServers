package services.http.forms

import models.Account
import models.Customer
import services.AppConfig
import utils._

class AccountSettingsFormTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest {

  import AccountSettingsForm.Fields

//  "Required parameters" should "be required" in {
//    val customer = TestData.newSavedCustomer()
//    val account = customer.account
//    val paramSet: Map[Fields.EnumVal, Iterable[String]] = Map(Fields.Fullname -> List(), Fields.Username -> List(), Fields.Email -> List(), Fields.GalleryVisibility -> List())
//
//    val accountSettingsForm = newForm(formReadable(paramSet), customer, account)
//    accountSettingsForm.fullname.error match {
//      case Some(_: ValueNotPresentFieldError) =>
//      case somethingElse =>
//        fail("" + somethingElse + " should have been a ValueNotPresentFieldError")
//    }
//    accountSettingsForm.username.error match {
//      case Some(_: ValueNotPresentFieldError) =>
//      case somethingElse =>
//        fail("" + somethingElse + " should have been a ValueNotPresentFieldError")
//    }
//    accountSettingsForm.email.error match {
//      case Some(_: ValueNotPresentFieldError) =>
//      case somethingElse =>
//        fail("" + somethingElse + " should have been a ValueNotPresentFieldError")
//    }
//    accountSettingsForm.galleryVisibility.error match {
//      case Some(_: ValueNotPresentFieldError) =>
//      case somethingElse =>
//        fail("" + somethingElse + " should have been a ValueNotPresentFieldError")
//    }
//  }

  /*
  Appropriate tests:
  required:
  fullname
  username
  email
  galleryVisibility

  optional:
  oldPassword
  newPassword
  passwordConfirm
  addressLine1
  addressLine2
  city
  state
  postalCode
  noticeStars

  username is unique
  email is unique
  email is an email
  oldPassword must match
  newPassword and passwordConfirm must match
  newPassword must have a valid password
  formAssumingValid spits out "false" if noticeStars does not have any value
  */

  private def newForm(readable: Form.Readable, customer: Customer, account: Account): AccountSettingsForm = {
    new AccountSettingsForm(readable, AppConfig.instance[FormChecks], customer, account)
  }

//  def formReadable(map: Map[Fields.EnumVal, Iterable[String]]): Form.Readable = {
//    (key: Fields.EnumVal) => map.get(key).getOrElse(List())
//  }
}
