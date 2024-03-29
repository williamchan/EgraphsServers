package services.http.forms

import com.google.inject.Inject
import models.{Password, Customer, Account}
import egraphs.playutils.Enum

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
        for (valid <- check.isAlphaNumeric(stringToValidate, "Username must be letters or numbers, no spaces").right;
             _ <- check.isUniqueUsername(stringToValidate, "Username already taken").right) yield valid
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
             _ <- check.isUniqueEmail(stringToValidate, "Email already taken").right) yield valid
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

  val noticeStars = new OptionalField[String](Fields.NoticeStars.name) {
    def validateIfPresent = Right(stringToValidate)
  }

  //
  // Form[ValidatedAccountSettingsForm] members
  //
  protected def formAssumingValid: AccountSettingsForm.Validated = {
    // Safely access the account value in here
    AccountSettingsForm.Validated(
      fullname = fullname.value.get,
      username = username.value.get,
      email = email.value.get,
      galleryVisibility = galleryVisibility.value.get,
      oldPassword = oldPassword.value.get.getOrElse(""),
      newPassword = newPassword.value.get.getOrElse(""),
      passwordConfirm = passwordConfirm.value.get.getOrElse(""),
      noticeStars = noticeStars.value.get.getOrElse("off"))
  }
}

object AccountSettingsForm {
  object Fields extends Enum {
    sealed case class EnumVal(name: String) extends Value

    // Strings need to match up with param names.
    val Fullname = EnumVal("fullname")
    val Username = EnumVal("username")
    val Email = EnumVal("email")
    val GalleryVisibility = EnumVal("galleryVisibility")
    val OldPassword = EnumVal("oldPassword")
    val NewPassword = EnumVal("newPassword")
    val PasswordConfirm = EnumVal("passwordConfirm")
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
