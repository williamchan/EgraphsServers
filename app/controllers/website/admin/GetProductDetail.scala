package controllers.website.admin

import play.mvc.Scope.Flash
import play.templates.Html
import models.Celebrity

object GetProductDetail {

  def getCelebrityProductDetail(celebrity: Celebrity, isCreate: Boolean)(implicit flash: Flash): Html = {
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)
    val fieldDefaults: (String => String) = {
      (paramName: String) => paramName match {
        case "productId" => flash.get("productId")
        case "productName" => flash.get("productName")
        case "productDescription" => flash.get("productDescription")
        case "storyTitle" => flash.get("storyTitle")
        case "storyText" => flash.get("storyText")
        case _ =>
          Option(flash.get(paramName)).getOrElse("")
      }
    }

    // Render the page
    views.Application.admin.html.admin_celebrityproductdetail(isCreate = isCreate, celebrity = celebrity, errorFields = errorFields, fields = fieldDefaults)
  }
}
