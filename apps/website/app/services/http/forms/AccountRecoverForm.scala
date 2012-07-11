package  services.http.forms

import services.Utils

import com.google.inject.Inject

class AccountRecoverForm(val paramsMap: Form.Readable, check: FormChecks) extends Form[AccountRecoverForm.Validated]
{
  import AccountRecoverForm.Fields

  val email = new RequiredField[String](Fields.Email.name) {
    def validateIfPresent = {
      check.isEmailAddress(stringToValidate)
    }
  }

  protected def formAssumingValid: AccountRecoverForm.Validated = {
    AccountRecoverForm.Validated(
      email = email.value.get
    )
  }
}

object AccountRecoverForm {
  object Fields extends Utils.Enum {
    sealed case class EnumVal(name: String) extends Value
    val Email = EnumVal("email")
  }

  case class Validated(email: String)

}

class AccountRecoverFormFactory @Inject()(formChecks: FormChecks){
  //
  // Public members
  //
  def apply(readable: Form.Readable): AccountRecoverForm = {
    new AccountRecoverForm(readable, formChecks)
  }

  def getFormReader : ReadsForm[AccountRecoverForm] = {
    new ReadsForm[AccountRecoverForm]() {
      override def instantiateAgainstReadable(readable: Form.Readable) = {
        new AccountRecoverForm(readable, formChecks)
      }
    }
  }
}