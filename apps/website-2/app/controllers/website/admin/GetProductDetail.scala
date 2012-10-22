package controllers.website.admin

import play.api.Play
import Play.current
import models.enums.PublishedStatus
import models.{Product, Celebrity}
import org.apache.commons.lang3.StringEscapeUtils
import play.api.mvc.Results.Ok

object GetProductDetail {

  def getCelebrityProductDetail(celebrity: Celebrity, isCreate: Boolean, product: Option[models.Product] = None
      )(implicit authToken: egraphs.authtoken.AuthenticityToken,
                 headerData: models.frontend.header.HeaderData, 
                 footerData: models.frontend.footer.FooterData, 
                 flash: play.api.mvc.Flash): play.api.mvc.Result = {
    
    val errorFields = flash.get("errors").map(errString => errString.split(',').toList)
    
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "productId" => flash.get("productId").getOrElse("")
        case "productName" => StringEscapeUtils.escapeHtml4(flash.get("productName").getOrElse(""))
        case "productDescription" => StringEscapeUtils.escapeHtml4(flash.get("productDescription").getOrElse(""))
        case "priceInCurrency" => flash.get("priceInCurrency").getOrElse("%.2f" format Product.defaultPrice)
        case "storyTitle" => if (isCreate) "The Story" else flash.get("storyTitle").getOrElse("")
        case "storyText" => {
          if (isCreate) "{signer_link}{signer_name}{end_link} defeated Dark Lord Sauron in 2012. A few days afterwards he got a note from {recipient_name} on his iPad. This was his response."
          else flash.get("storyText").getOrElse("")
        }
        case "publishedStatusString" => flash.get("publishedStatusString").getOrElse(PublishedStatus.Unpublished.toString)
        case _ => flash.get(paramName).getOrElse("")
      }
    }

    Ok(views.html.Application.admin.admin_celebrityproductdetail(
      isCreate = isCreate,
      celebrity = celebrity,
      errorFields = errorFields,
      fields = fieldDefaults,
      product = product,
      isTestMode = !Play.isProd))
  }
}
