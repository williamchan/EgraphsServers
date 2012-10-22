package controllers.website.admin

import models._
import enums.PublishedStatus
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.format.Formats._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import java.io.File
import services.logging.Logging
import services.ImageUtil
import play.api.Play
import java.text.SimpleDateFormat
import services.Dimensions
import play.api.mvc.MultipartFormData
import java.awt.image.BufferedImage

// TODO(wchan): This code has become frankenstein. I'm so sorry.
trait PostCelebrityProductAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  case class PostProductForm(
    productName: String,
    productDescription: String,
    priceInCurrency: String,
    signingOriginX: Int,
    signingOriginY: Int,
    storyTitle: String,
    storyText: String,
    publishedStatusString: String
  )

  def postCelebrityProductAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebId = celebrityId, parser = parse.multipartFormData) { celebrity =>
      	Action(parse.multipartFormData) { implicit request =>
      	  
      	  val productId = Form("productId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
      	  val createWithoutInventory = Form("createWithoutInventory" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
      	  val productImageFile = request.body.file("productImage").map(_.ref.file)
      	  val productIconFile = request.body.file("productIcon").map(_.ref.file)
      	  val productImageOption = if (productImageFile.isDefined) ImageUtil.parseImage(productImageFile.get) else None
      	  val productIconOption = if (productIconFile.isDefined) ImageUtil.parseImage(productIconFile.get) else None
      	  val isCreate = (productId == 0)

      	  val form = Form(mapping(
              "productName" -> nonEmptyText(maxLength = 128),        // 128 is the database column width
              "productDescription" -> nonEmptyText(maxLength = 128), // 128 is the database column width
              "priceInCurrency" -> nonEmptyText.verifying(isDouble),
              "signingOriginX" -> number(min = 0, max = 575), // These validations assume that the product is landscape.
              "signingOriginY" -> number(min = 0, max = 0),   // These validations assume that the product is landscape.
              "storyTitle" -> nonEmptyText,
              "storyText" -> nonEmptyText,
              "publishedStatusString" -> nonEmptyText.verifying(isProductPublishedStatus)
          )(PostProductForm.apply)(PostProductForm.unapply)
            .verifying(
                productNameIsUnique(celebrity, productId),
                productImageIsRequiredForCreate(isCreate, productImageFile),
                productImageIsValid(productImageOption, isCreate, productImageFile),
                productIconIsValid(productIconOption, productIconFile))
          )

          form.bindFromRequest.fold(
            formWithErrors => {
              val data = formWithErrors.data
              val errors = for (error <- formWithErrors.errors) yield {
                error.key + ": " + error.message + ". Found: " + error.args.toString
              }
              val url = if (isCreate) GetCreateCelebrityProductAdminEndpoint.url(celebrity = celebrity) else GetProductAdminEndpoint.url(productId = productId)
              Redirect(url).flashing(
                ("errors" -> errors.mkString(", ")), 
		        ("productId" -> productId.toString), 
		        ("productName" -> data.get("productName").getOrElse("")), 
		        ("productDescription" -> data.get("productDescription").getOrElse("")), 
		        ("priceInCurrency" -> data.get("priceInCurrency").getOrElse("")), 
		        ("signingOriginX" -> data.get("signingOriginX").getOrElse("")), 
		        ("signingOriginY" -> data.get("signingOriginY").getOrElse("")), 
		        ("storyTitle" -> data.get("storyTitle").getOrElse("")), 
		        ("storyText" -> data.get("storyText").getOrElse("")), 
		        ("publishedStatusString" -> data.get("publishedStatusString").getOrElse(""))
              )
            },
            validForm => {
              val publishedStatus = PublishedStatus(validForm.publishedStatusString).getOrElse(PublishedStatus.Unpublished)
              val savedProduct = if (isCreate) {
		          celebrity.addProduct(
		            name = validForm.productName,
		            description = validForm.productDescription,
		            priceInCurrency = BigDecimal(validForm.priceInCurrency.toDouble),
		            image = productImageOption,
		            icon = productIconOption,
		            storyTitle = validForm.storyTitle,
		            storyText = validForm.storyText,
		            publishedStatus = publishedStatus
		          ).copy(signingOriginX = validForm.signingOriginX, signingOriginY = validForm.signingOriginY)
	          } else {
		          val product = productStore.get(productId)
		          product.copy(
		            name = validForm.productName,
		            description = validForm.productDescription,
		            priceInCurrency = BigDecimal(validForm.priceInCurrency.toDouble),
		            signingOriginX = validForm.signingOriginX,
		            signingOriginY = validForm.signingOriginY,
		            storyTitle = validForm.storyTitle,
		            storyText = validForm.storyText
		          ).withPublishedStatus(publishedStatus).saveWithImageAssets(image = productImageOption, icon = productIconOption)
	          }
              maybeCreateInventoryBatchForDemoMode(savedProduct, isCreate, createWithoutInventory)
              Redirect(controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug).url)
            }
          )
      	}
      }
    }
  }

  /**
   * This is here so that demo'ers don't need to worry about setting up an InventoryBatch for demo Products before making orders.
   */
  private def maybeCreateInventoryBatchForDemoMode(product: Product, isCreate: Boolean, createWithoutInventory: String) {
    if (isCreate && createWithoutInventory.isEmpty && !Play.isProd(Play.current)) {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
      val jan_01_2012 = dateFormat.parse("2012-01-01")
      val future = dateFormat.parse("2020-01-01")
      val inventoryBatch = InventoryBatch(celebrityId = product.celebrityId, numInventory = 100, startDate = jan_01_2012, endDate = future).save()
      inventoryBatch.products.associate(product)
    }
  }

  private def productNameIsUnique(celebrity: Celebrity, productId: Long): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      val productByUrlSlg = productStore.findByCelebrityAndUrlSlug(celebrity.id, Product.slugify(form.productName))
      val isCreate = (productId == 0)
      val isUniqueUrlSlug = if (isCreate) {
        productByUrlSlg.isEmpty
      } else {
        productByUrlSlg.isEmpty || (productByUrlSlg.isDefined && productByUrlSlg.get.id == productId)
      }
      if (isUniqueUrlSlug) Valid else Invalid("Celebrity already has a product with name: " + form.productName)
    }
  }

  private def productImageIsRequiredForCreate(isCreate: Boolean, productImageFile: Option[File]): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      if (isCreate) {
        if (productImageFile.isDefined) Valid else Invalid("Product image is required")
      } else {
        Valid
      }
    }
  }

  private def productImageIsValid(productImageOption: Option[BufferedImage], isCreate: Boolean, productImageFile: Option[File]): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      val isProductImageValid = (isCreate && !productImageOption.isEmpty) || (!isCreate && (productImageFile.isEmpty || !productImageOption.isEmpty))
      if (isProductImageValid) {
        val validationResult = for (image <- productImageOption) yield {
	        val (width, height) = (image.getWidth, image.getHeight)
	        val resolutionStr = width + "x" + height
	        val isOriginalImageTooSmall = width < Product.minPhotoWidth || height < Product.minPhotoHeight
	        if (isOriginalImageTooSmall) {
	          Invalid("Product Photo must be at least " + Product.minPhotoWidth + " in width and " + Product.minPhotoHeight + " in height - resolution was " + resolutionStr)
	        }
	        else {
	          val croppedDimensions = EgraphFrame.suggestedFrame(Dimensions(width, height)).getCropDimensions(image)
	          if (!(croppedDimensions.width < Product.minPhotoWidth || croppedDimensions.height < Product.minPhotoHeight)) {
	            Valid
	          } else {
	            Invalid("Product Photo must be at just a bit larger because it would be cropped to below 1024 on one side. "
	              + "Please upscale the image (you can do this with Mac Preview) or find a larger image")
	          }
	        }
        }
        validationResult.getOrElse(Valid)
      } else {
        Invalid("Product photo must be a valid image")
      }
    }
  }

  private def productIconIsValid(productIconOption: Option[BufferedImage], productIconFile: Option[File]): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
	  val isProductIconValid = (productIconFile.isEmpty || !productIconOption.isEmpty)
	  if (!isProductIconValid) {
	    Invalid("Product icon must be a valid image")
	  } else {
	    val validationResult = for (image <- productIconOption) yield {
	      if (image.getWidth >= Product.minIconWidth && image.getHeight >= Product.minIconWidth) Valid else Invalid("Product icon must be at least 40px wide and 40px high")
	    }
	    validationResult.getOrElse(Valid)
	  }
    }
  }

  // TODO: redundant with isCelebrityPublishedStatus
  private def isProductPublishedStatus: Constraint[String] = {
    Constraint { s: String =>
      PublishedStatus(s) match {
        case Some(providedStatus) => Valid
        case None => Invalid("Error setting product's published status, please contact support")
      }
    }
  }

  // TODO: PLAY20 migration: This should ideally be a Forms mapping like text or number.
  def isDouble: Constraint[String] = {
    Constraint { s: String =>
      try {
        s.toDouble
        Valid
      } catch { case _ => Invalid("Expected a decimal number, but got: " + s) }
    }
  }
}
