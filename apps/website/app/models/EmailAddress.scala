package models

import play.api.libs.json._
import play.api.data.Forms._
import play.api.data.Form

case class EmailAddress(value: String)

object EmailAddress {
  def isValid(emailAddress: String): Boolean = {
    !Form("email" -> email).bind(Map("email" -> emailAddress)).hasErrors
  }

  /**
   * This will let you more easily write email addresses to your json objects
   * using the api formatting.
   */
  implicit object EmailAddressFormat extends Format[EmailAddress] {
    def writes(emailAddress: EmailAddress): JsValue = {
      JsString(emailAddress.value)
    }

    def reads(json: JsValue): JsResult[EmailAddress] = {
      val emailAddressString = json.as[String]
      if (EmailAddress.isValid(emailAddressString)) {
        JsSuccess {
          EmailAddress(emailAddressString)
        }
      } else {
        JsError(s"Not a valid email address = emailAddressString")
      }
    }
  }
}
