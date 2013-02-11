package services.mail

import models.enums.EmailType
import models.frontend.email._

object MailUtils {
  
  def baseList(title: String) = List(("title", title))
  
  //TODO: think about whether EmailType enum is necessary or not
  def getAccountVerificationTemplateContentParts(emailType: EmailType.EnumVal, accountVerificationEmailStack: AccountVerificationEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_verification", views.html.frontend.tags.email.account_verification(accountVerificationEmailStack).body))
  }

  def getAccountConfirmationTemplateContentParts(emailType: EmailType.EnumVal): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_confirmation", views.html.frontend.tags.email.account_confirmation().body))
  }

  def getResetPasswordTemplateContentParts(emailType: EmailType.EnumVal, resetPasswordEmailStack: ResetPasswordEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("reset_password", views.html.frontend.tags.email.reset_password(resetPasswordEmailStack).body))
  }

  def getCelebrityWelcomeTemplateContentParts(emailType: EmailType.EnumVal, celebrityWelcomeEmailStack: CelebrityWelcomeEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("celebrity_welcome", views.html.frontend.tags.email.celebrity_welcome(celebrityWelcomeEmailStack).body))
  }

  def getViewEgraphTemplateContentParts(emailType: EmailType.EnumVal, viewEgraphEmailStack: ViewEgraphEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("view_egraph", views.html.frontend.tags.email.view_egraph(viewEgraphEmailStack).body))
  }

  def getOrderConfirmationTemplateContentParts(emailType: EmailType.EnumVal, orderConfirmationEmailStack: OrderConfirmationEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) :::
    List(("order_confirmation", views.html.frontend.tags.email.order_confirmation(orderConfirmationEmailStack).body))
  }
}