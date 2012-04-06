package controllers.website.admin

import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.mvc.Controller
import java.io.File
import play.data.validation.Validation.required
import controllers.website.GetCelebrityProductEndpoint
import play.data.validation.Validation
import models._
import services.logging.Logging
import services.{ImageUtil}
import services.http.{ControllerMethod, CelebrityAccountRequestFilters, AdminRequestFilters}

trait PostCelebrityProductEndpoint extends Logging {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  def postCelebrityProduct(productId: Long = 0,
                           productName: String,
                           productDescription: String,
                           productImage: File,
                           productIcon: File,
                           storyTitle: String,
                           storyText: String) = controllerMethod() {
    celebFilters.requireCelebrityId(request) { celebrity =>
      val isCreate = (productId == 0)

      // Validate text fields
      required("Product name", productName)
      required("Product image", productImage)
      required("Product icon", productIcon)
      required("Story title", storyTitle)
      required("Story text", storyText)
      val productByUrlSlg = productStore.findByCelebrityAndUrlSlug(celebrity.id, Product.slugify(productName))
      val isUniqueUrlSlug = if (isCreate) {
        productByUrlSlg.isEmpty
      } else {
        productByUrlSlg.isEmpty || (productByUrlSlg.isDefined && productByUrlSlg.get.id == productId)
      }
      Validation.isTrue("Celebrity already has a product with name: " + productName, isUniqueUrlSlug)

      // Validate product image
      val productImageOption = ImageUtil.parseImage(productImage)
      Validation.isTrue("Product photo must be a valid image", !productImageOption.isEmpty)
      for (image <- productImageOption) {
        val (width, height) = (image.getWidth, image.getHeight)

        val resolutionStr = width + "x" + height

        Validation.isTrue(
          "Product Photo must be at least 940 in width and 900 in height - resolution was " + resolutionStr,
          width >= 940 && height >= 900
        )
      }

      // Validate product icon
      val productIconOption = ImageUtil.parseImage(productIcon)
      Validation.isTrue("Product icon must be a valid image", !productIconOption.isEmpty)
      for (image <- productIconOption) {
        Validation.isTrue(
          "Product icon must be at least 41px wide and 41px high",
          image.getWidth >= 40 && image.getHeight >= 40
        )
      }

      // All errors are accumulated. If we have no validation errors then parameters are golden and
      // we delegate creating the Product to the Celebrity.
      if (validationErrors.isEmpty) {
        log("Request to create product \"" + productName + "\" for celebrity " + celebrity.publicName + " passed all filters.")
        val savedProduct = if (isCreate) {
          celebrity.addProduct(
            name=productName,
            description=productDescription,
            image=productImageOption.get,
            icon=productIconOption.get,
            storyTitle=storyTitle,
            storyText=storyText
          )
        } else {
          val product = productStore.findById(productId).get
          product.copy(
            name=productName,
            description=productDescription,
            storyTitle=storyTitle,
            storyText=storyText
          ).saveWithImageAssets(image = productImageOption.get, icon = productIconOption.get)
        }

        new Redirect(GetCelebrityProductEndpoint.url(celebrity, savedProduct).url)
      }
      else {
        // There were validation errors
        redirectWithValidationErrors(celebrity, productId, productName, productDescription, storyTitle, storyText)
      }
    }
  }

  private def redirectWithValidationErrors(celebrity: Celebrity,
                                           productId: Long,
                                           productName: String,
                                           productDescription: String,
                                           storyTitle: String,
                                           storyText: String): Redirect = {
    flash.put("productId", productId)
    flash.put("productName", productName)
    flash.put("productDescription", productDescription)
    flash.put("storyTitle", storyTitle)
    flash.put("storyText", storyText)
    if (productId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductEndpoint.url(celebrity = celebrity))
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetUpdateCelebrityProductEndpoint.url(celebrity = celebrity, productId = productId))
    }
  }
}