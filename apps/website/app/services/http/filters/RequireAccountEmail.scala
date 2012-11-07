package services.http.filters

import com.google.inject.Inject

import models.Account
import models.AccountStore
import play.api.data.Forms.email
import play.api.data.Forms.single
import play.api.data.Form
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

class RequireAccountEmail @Inject() (accountStore: AccountStore) extends Filter[String, Account] with RequestFilter[String, Account] {
  val notFound = NotFound("Account not found")

  override def filter(email: String): Either[Result, Account] = {
    accountStore.findByEmail(email) match {
      case Some(account) => Right(account)
      case _ => Left(notFound)
    }
  }

  override val form: Form[String] = Form(single("email" -> email))
}
