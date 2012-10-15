package controllers.website.admin

import play.api.mvc.Results.Redirect
import controllers.WebsiteControllers
import play.api.mvc.Controller
import java.io.File
import play.data.validation.Validation.required
import play.data.validation.Validation
import models._
import enums.PublishedStatus
import services.logging.Logging
import services.ImageUtil
import play.Play
import java.text.SimpleDateFormat
import services.http.{POSTControllerMethod, CelebrityAccountRequestFilters, AdminRequestFilters}
import services.http.SafePlayParams.Conversions._
import services.Dimensions
import models.InventoryBatch
import scala.Some

trait PostCelebrityProductAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  def postCelebrityProductAdmin(productId: Long = 0,
                                productName: String,
                                productDescription: String,
                                priceInCurrency: Double,
                                productImage: Option[File],
                                productIcon: Option[File],
                                signingOriginX: Int,
                                signingOriginY: Int,
                                storyTitle: String,
                                storyText: String,
                                publishedStatusString: String) = postController() {
    celebFilters.requireCelebrityId(request) { celebrity =>
      val isCreate = (productId == 0)

      // Validate text fields
      required("Product name", productName)
      if (isCreate) Validation.isTrue("Product image is required", productImage.isDefined)
      required("Story title", storyTitle)
      required("Story text", storyText)

      Validation.isTrue("Product name has maximum length of 128", productName.length < 128)                // column width in database
      Validation.isTrue("Product description has maximum length of 128", productDescription.length < 128)  // column width in database

      // These validations assume that the product is landscape.
      Validation.isTrue("Signing Origin X must be between 0 and 575", signingOriginX >= 0 && signingOriginX <= 575)
      Validation.isTrue("Leave Signing Origin Y as 0 ", signingOriginY == 0)

      val productByUrlSlg = productStore.findByCelebrityAndUrlSlug(celebrity.id, Product.slugify(productName))
      val isUniqueUrlSlug = if (isCreate) {
        productByUrlSlg.isEmpty
      } else {
        productByUrlSlg.isEmpty || (productByUrlSlg.isDefined && productByUrlSlg.get.id == productId)
      }
      Validation.isTrue("Celebrity already has a product with name: " + productName, isUniqueUrlSlug)

      // Validate product image
      val productImageOption = if (productImage.isDefined) ImageUtil.parseImage(productImage.get) else None
      val isProductImageValid = (isCreate && !productImageOption.isEmpty) || (!isCreate && (productImage.isEmpty || !productImageOption.isEmpty))
      Validation.isTrue("Product photo must be a valid image", isProductImageValid)
      for (image <- productImageOption) {
        val (width, height) = (image.getWidth, image.getHeight)
        val resolutionStr = width + "x" + height
        val isOriginalImageTooSmall = width < Product.minPhotoWidth || height < Product.minPhotoHeight
        Validation.isTrue(
          "Product Photo must be at least " + Product.minPhotoWidth + " in width and " + Product.minPhotoHeight + " in height - resolution was " + resolutionStr,
          !isOriginalImageTooSmall
        )
        if (!isOriginalImageTooSmall) {
          val croppedDimensions = EgraphFrame.suggestedFrame(Dimensions(width, height)).getCropDimensions(image)
          Validation.isTrue(
            "Product Photo must be at just a bit larger because it would be cropped to below 1024 on one side. "
              + "Please upscale the image (you can do this with Mac Preview) or find a larger image",
            !(croppedDimensions.width < Product.minPhotoWidth || croppedDimensions.height < Product.minPhotoHeight)
          )
        }
      }

      // Validate product icon
      val productIconOption = if (productIcon.isDefined) ImageUtil.parseImage(productIcon.get) else None
      val isProductIconValid = (productIcon.isEmpty || !productIconOption.isEmpty)
      Validation.isTrue("Product icon must be a valid image", isProductIconValid)
      for (image <- productIconOption) {
        Validation.isTrue(
          "Product icon must be at least 40px wide and 40px high",
          image.getWidth >= Product.minIconWidth && image.getHeight >= Product.minIconWidth
        )
      }

      //publishedStatusString validation
      val publishedStatus = PublishedStatus(publishedStatusString) match {
        case Some(providedStatus) =>
          providedStatus
        case None =>
          Validation.addError("Error setting product's published status, please contact support", "")
          PublishedStatus.Unpublished
      }

      // All errors are accumulated. If we have no validation errors then parameters are golden and
      // we delegate creating the Product to the Celebrity.
      if (validationErrors.isEmpty) {
        val savedProduct = if (isCreate) {
          celebrity.addProduct(
            name = productName,
            description = productDescription,
            priceInCurrency = BigDecimal(priceInCurrency),
            image = productImageOption,
            icon = productIconOption,
            storyTitle = storyTitle,
            storyText = storyText,
            publishedStatus = publishedStatus
          ).copy(signingOriginX = signingOriginX, signingOriginY = signingOriginY)
        } else {
          val product = productStore.get(productId)
          product.copy(
            name = productName,
            description = productDescription,
            priceInCurrency = BigDecimal(priceInCurrency),
            signingOriginX = signingOriginX,
            signingOriginY = signingOriginY,
            storyTitle = storyTitle,
            storyText = storyText
          ).withPublishedStatus(publishedStatus).saveWithImageAssets(image = productImageOption, icon = productIconOption)
        }

        maybeCreateInventoryBatchForDemoMode(savedProduct, isCreate)
        new Redirect(WebsiteControllers.reverse(WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug)).url)
      }
      else {
        // There were validation errors
        redirectWithValidationErrors(celebrity, productId, productName, productDescription, signingOriginX, signingOriginY, storyTitle, storyText, publishedStatusString)
      }
    }
    }


  /**
   * This is here so that demo'ers don't need to worry about setting up an InventoryBatch for demo Products before making orders.
   */
  private def maybeCreateInventoryBatchForDemoMode(product: Product, isCreate: Boolean) {
    if (isCreate && (params.getOption("createWithoutInventory").isEmpty) && (Play.id == "test")) {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
      val jan_01_2012 = dateFormat.parse("2012-01-01")
      val future = dateFormat.parse("2020-01-01")
      val inventoryBatch = InventoryBatch(celebrityId = product.celebrityId, numInventory = 100, startDate = jan_01_2012, endDate = future).save()
      inventoryBatch.products.associate(product)
    }
  }

  private def redirectWithValidationErrors(celebrity: Celebrity,
                                           productId: Long,
                                           productName: String,
                                           productDescription: String,
                                           signingOriginX: Int,
                                           signingOriginY: Int,
                                           storyTitle: String,
                                           storyText: String,
                                           publishedStatusString: String): Redirect = {
    flash.put("productId", productId)
    flash.put("productName", productName)
    flash.put("productDescription", productDescription)
    flash.put("signingOriginX", signingOriginX)
    flash.put("signingOriginY", signingOriginY)
    flash.put("storyTitle", storyTitle)
    flash.put("storyText", storyText)
    flash.put("publishedStatusString", publishedStatusString)
    if (productId == 0) {
      WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductAdminEndpoint.url(celebrity = celebrity))
    } else {
      WebsiteControllers.redirectWithValidationErrors(GetProductAdminEndpoint.url(productId = productId))
    }
  }
}