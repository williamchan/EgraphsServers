package services.http.forms

import com.google.inject.Inject
import services.Utils
import models.{Password, Customer, Account}

/**
 * Define the fields (and validations thereof) for the form transmitted by this endpoint
 *
 * @param paramsMap either the request parameters or flash parameters converted to Readable
 *     by Form.Conversions._
 *
 * @param check the checks used by forms in our application.
 **/
class AccountSettingsForm(val paramsMap: Form.Readable, check: FormChecks, customer: Customer, account: Account)
  extends Form[AccountSettingsForm.Validated]
{
  import AccountSettingsForm.Fields

  //
  // Field values and validations
  //
  val fullname = new RequiredField[String](Fields.Fullname.name) {
    def validateIfPresent = Right(stringToValidate)
    override def errorMessage = "Name is required"
  }

  val username = new RequiredField[String](Fields.Username.name) {
    def validateIfPresent = {
      if (customer.username == stringToValidate) {
        Right(stringToValidate)
      } else {
        for (valid <- check.isAlphaNumeric(stringToValidate, "Username must one word and letters or numbers").right;
             valid2 <- check.isUniqueUsername(stringToValidate, "Username already taken").right) yield valid
      }
    }
    override def errorMessage = "Username is required"
  }

  val email = new RequiredField[String](Fields.Email.name) {
    def validateIfPresent = {
      if (account.email == stringToValidate) {
        Right(stringToValidate)
      } else {
        for (valid <- check.isEmailAddress(stringToValidate).right;
             valid2 <- check.isUniqueEmail(stringToValidate, "Email already taken").right) yield valid
      }
    }
    override def errorMessage = "Email is required"
  }

  val galleryVisibility = new RequiredField[String](Fields.GalleryVisibility.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  val oldPassword = new OptionalField[String](Fields.OldPassword.name) {
    def validateIfPresent = {
      account.password match {
        case Some(pw) if pw.is(stringToValidate) => Right(stringToValidate)
        case _ => Left(new SimpleFormError("Password is incorrect"))
      }
    }
  }
  val newPassword = new OptionalField[String](Fields.NewPassword.name) {
    def validateIfPresent = {
      Password.validate(stringToValidate) match {
        case Left(validationResult) => Left(new SimpleFormError("Password must be at least 8 characters"))
        case _ => Right(stringToValidate)
      }
    }
  }
  val passwordConfirm = new OptionalField[String](Fields.PasswordConfirm.name) {
    def validateIfPresent = {
      newPassword.value match {
        case Some(Some(password)) if password == stringToValidate => Right(stringToValidate)
        case _ => Left(new SimpleFormError("New passwords do not match"))
      }
    }
  }

  val addressLine1 = new OptionalField[String](Fields.AddressLine1.name) {
    def validateIfPresent = Right(stringToValidate)
  }
  val addressLine2 = new OptionalField[String](Fields.AddressLine2.name) {
    def validateIfPresent = Right(stringToValidate)
  }
  val city = new OptionalField[String](Fields.City.name) {
    def validateIfPresent = Right(stringToValidate)
  }
  val state = new OptionalField[String](Fields.State.name) {
    def validateIfPresent = Right(stringToValidate)
  }
  val postalCode = new OptionalField[String](Fields.PostalCode.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  val noticeStars = new OptionalField[String](Fields.NoticeStars.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  //
  // Form[ValidatedAccountSettingsForm] members
  //
  protected def formAssumingValid: AccountSettingsForm.Validated = {
    // todo(wchan): Where does this weird string come from??
    val stateStr = state.value.get match {
      case Some("? string: ?") => ""
      case _ => state.value.get.getOrElse("")
    }
    // Safely access the account value in here
    AccountSettingsForm.Validated(
      fullname = fullname.value.get,
      username = username.value.get,
      email = email.value.get,
      galleryVisibility = galleryVisibility.value.get,
      oldPassword = oldPassword.value.get.getOrElse(""),
      newPassword = newPassword.value.get.getOrElse(""),
      passwordConfirm = passwordConfirm.value.get.getOrElse(""),
      addressLine1 = addressLine1.value.get.getOrElse(""),
      addressLine2 = addressLine2.value.get.getOrElse(""),
      city = city.value.get.getOrElse(""),
      state = stateStr,
      postalCode = postalCode.value.get.getOrElse(""),
      noticeStars = noticeStars.value.get.getOrElse("false"))
  }
}

object AccountSettingsForm {
  object Fields extends Utils.Enum {
    sealed case class EnumVal(name: String) extends Value

    // Strings need to match up with param names.
    val Fullname = EnumVal("fullname")
    val Username = EnumVal("username")
    val Email = EnumVal("email")
    val GalleryVisibility = EnumVal("galleryVisibility")
    val OldPassword = EnumVal("oldPassword")
    val NewPassword = EnumVal("newPassword")
    val PasswordConfirm = EnumVal("passwordConfirm")
    val AddressLine1 = EnumVal("address.line1")
    val AddressLine2 = EnumVal("address.line2")
    val City = EnumVal("address.city")
    val State = EnumVal("address.state")
    val PostalCode = EnumVal("address.postalCode")
    val NoticeStars = EnumVal("notices.new_stars")
  }

  /** Class to which the fully validated AccountSettingsForm resolves */
  case class Validated(fullname: String,
                       username: String,
                       email: String,
                       galleryVisibility: String,
                       oldPassword: String,
                       newPassword: String,
                       passwordConfirm: String,
                       addressLine1: String,
                       addressLine2: String,
                       city: String,
                       state: String,
                       postalCode: String,
                       noticeStars: String)
}

/**
 * Factory that reads the AccountSettingsForm from either the request parameters (for a POST)
 * or the flash (for a redirected GET)
 *
 * @param formChecks checks used to validate the POST.
 */
class AccountSettingsFormFactory @Inject()(formChecks: FormChecks)
{
  //
  // Public members
  //
  def apply(readable: Form.Readable, customer: Customer, account: Account): AccountSettingsForm = {
    new AccountSettingsForm(readable, formChecks, customer, account)
  }

  /**
   * Need a specialized form reader her to pass in Customer and Account.
   */
  def getFormReader(customer: Customer, account: Account): ReadsForm[AccountSettingsForm] = {
    new ReadsForm[AccountSettingsForm]() {
      override def instantiateAgainstReadable(readable: Form.Readable) = {
        new AccountSettingsForm(readable, formChecks, customer, account)
      }
    }
  }
}
