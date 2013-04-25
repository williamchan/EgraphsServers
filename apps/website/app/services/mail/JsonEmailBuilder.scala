package services.mail

import models.frontend.email.EmailViewModel
import play.api.libs.json.JsValue
import play.api.libs.json.Json

object JsonEmailBuilder {

  val bccAddress = "email-records@egraphs.com"

  def sendTemplateJson(mailStack: EmailViewModel, templateContentParts: List[(String, String)], key: String, html : String =""): JsValue = {
    Json.toJson(Map(
      "key" -> Json.toJson(key),
      "message" -> Json.toJson(
        Map(
          "subject" -> Json.toJson(mailStack.subject),
          "html" -> Json.toJson(html),
          "text" -> Json.toJson("derp"),
          "from_email" -> Json.toJson(mailStack.fromEmail),
          "from_name" -> Json.toJson(mailStack.fromName),
          "to" -> Json.toJson(getToAddresses(mailStack)),
          "headers" -> Json.toJson(
            Map(
              "Reply-To" -> Json.toJson(mailStack.replyToEmail)
            )
          ),
          "track_opens" -> Json.toJson(true),
          "track_clicks" -> Json.toJson(true),
          "auto_text" -> Json.toJson(true),
          "bcc_address" -> Json.toJson(bccAddress)
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
