package services.mail

import models.frontend.email.EmailViewModel
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object JsonEmailBuilder {
  
  def sendTemplateJson(mailStack: EmailViewModel, templateContentParts: List[(String, String)], key: String): JsValue = {
    Json.toJson(Map(
      "key" -> Json.toJson(key),
      "template_name" -> Json.toJson("General"),
      "template_content" -> Json.toJson(getTemplateContentPieces(templateContentParts)),
      "message" -> Json.toJson(
        Map(
          "subject" -> Json.toJson(mailStack.subject),
          "from_email" -> Json.toJson(mailStack.fromEmail),
          "from_name" -> Json.toJson(mailStack.fromName),
          "to" -> Json.toJson(getToAddresses(mailStack)),
          "headers" -> Json.toJson(
            Map(
              "Reply-To" -> Json.toJson(mailStack.replyToEmail)
            )
          ),
          "auto_text" -> Json.toJson(true),
          "bcc_address" -> Json.toJson(mailStack.bccAddress.getOrElse(""))
        )
      ),
      "async" -> Json.toJson(true)
    ))
  }

  def getTemplateContentPieces(templateContentPieces: List[(String, String)]): Seq[JsValue] = {
    for ((name, content) <- templateContentPieces) yield {
      Json.toJson(
        Map(
          "name" -> Json.toJson(name),
          "content" -> Json.toJson(content)
        )
      )
    }
  }

  def getToAddresses(mailStack: EmailViewModel): Seq[JsValue] = {
    for ((email, name) <- mailStack.toAddresses) yield {
      Json.toJson(
        Map(
          "email" -> Json.toJson(email),
          "name" -> Json.toJson(name.getOrElse(email)) // use email if no name given
        )
      )
    }
  }  

}