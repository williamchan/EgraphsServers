package controllers.website.admin

import play.templates.Html
import play.mvc.Scope.{Session, Flash}
import play.Play
import models.enums.PublishedStatus
import models.Celebrity

object GetProductDetail {

  def getCelebrityProductDetail(celebrity: Celebrity, isCreate: Boolean, product: Option[models.Product] = None)(implicit flash: Flash, session: Session): Html = {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "productId" => flash.get("productId")
        case "productName" => flash.get("productName")
        case "productDescription" => flash.get("productDescription")
        case "storyTitle" => if (isCreate) "The Story" else flash.get("storyTitle")
        case "storyText" => {
          if (isCreate) "{signer_link}{signer_name}{end_link} defeated Dark Lord Sauron in 2012. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response."
          else flash.get("storyText")
        }
        case "publishedStatusString" =>
          Option(flash.get("publishedStatusString")).getOrElse(PublishedStatus.Unpublished.name)
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }

    // Render the page
    views.Application.admin.html.admin_celebrityproductdetail(
      isCreate = isCreate,
      celebrity = celebrity,
      errorFields = errorFields,
      fields = fieldDefaults,
      product = product,
      isTestMode = (Play.id == "test"))
  }
}
