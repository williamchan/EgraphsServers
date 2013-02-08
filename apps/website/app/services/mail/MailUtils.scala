package services.mail

import models.enums.EmailType
import models.frontend.email.AccountVerificationEmailViewModel

object MailUtils {
  
  def baseList(title: String) = List(("title", title))
  
  //TODO: think about whether EmailType enum is necessary or not
  def getAccountVerificationTemplateContentParts(emailType: EmailType.EnumVal, accountVerificationEmailStack: AccountVerificationEmailViewModel): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_verification", views.html.frontend.tags.email.account_verification(accountVerificationEmailStack.verifyPasswordUrl).toString))
  }
  
  def getAccountConfirmationTemplateContentParts(emailType: EmailType.EnumVal): List[(String, String)] = {
    baseList(emailType.name) ::: 
    List(("account_confirmation", views.html.frontend.tags.email.account_confirmation().toString))
  }  
}