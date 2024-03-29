package controllers.website.admin

import models._
import enums.PublishedStatus
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
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

trait PostProductAdminEndpoint extends Logging {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  def postCreateProductAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { case (admin, adminAccount) =>
      httpFilters.requireCelebrityId(celebrityId, parse.multipartFormData) { celebrity =>
      	Action(parse.multipartFormData) { implicit request =>
      	  
      	  val createWithoutInventory = Form("createWithoutInventory" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
      	  
      	  val (productImageFile, productIconFile, productImageOption, productIconOption) = getUploadedImages(request.body)
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
                productNameIsUnique(celebrity),
                productImageIsRequiredForCreate(productImageFile),
                productImageIsValid(productImageOption = productImageOption, productImageFile = productImageFile, isCreate = true),
                productIconIsValid(productIconOption, productIconFile))
          )

          form.bindFromRequest.fold(
            formWithErrors => {
              val data = formWithErrors.data
              val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
              val url = GetCreateCelebrityProductAdminEndpoint.url(celebrity = celebrity)
              Redirect(url).flashing(
                ("errors" -> errors.mkString(", ")), 
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
              val savedProduct = celebrity.addProduct(
		            name = validForm.productName,
		            description = validForm.productDescription,
		            priceInCurrency = BigDecimal(validForm.priceInCurrency.toDouble),
		            image = productImageOption,
		            icon = productIconOption,
		            storyTitle = validForm.storyTitle,
		            storyText = validForm.storyText,
		            publishedStatus = publishedStatus
		          ).copy(signingOriginX = validForm.signingOriginX, signingOriginY = validForm.signingOriginY)
		          
              maybeCreateInventoryBatchForDemoMode(savedProduct, createWithoutInventory)
              Redirect(controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug).url)
            }
          )
      	}
      }
    }
  }
  
  def postProductAdmin(productId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession(parser = parse.multipartFormData) { case (admin, adminAccount) =>
      httpFilters.requireProductId(productId, parser = parse.multipartFormData) { product =>
      	Action(parse.multipartFormData) { implicit request =>
      	  
      	  val celebrity = product.celebrity
      	  val createWithoutInventory = Form("createWithoutInventory" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)

      	  val (productImageFile, productIconFile, productImageOption, productIconOption) = getUploadedImages(request.body)
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
                productNameIsUnique(celebrity, Some(productId)),
                productImageIsValid(productImageOption = productImageOption, productImageFile = productImageFile, isCreate = false),
                productIconIsValid(productIconOption, productIconFile))
          )

          form.bindFromRequest.fold(
            formWithErrors => {
              val data = formWithErrors.data
              val errors = for (error <- formWithErrors.errors) yield { error.key + ": " + error.message }
              val url = GetProductAdminEndpoint.url(productId = productId)
              Redirect(url).flashing(
                ("errors" -> errors.mkString(", ")), 
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
              val savedProduct = product.copy(
		            name = validForm.productName,
		            description = validForm.productDescription,
		            priceInCurrency = BigDecimal(validForm.priceInCurrency.toDouble),
		            signingOriginX = validForm.signingOriginX,
		            signingOriginY = validForm.signingOriginY,
		            storyTitle = validForm.storyTitle,
		            storyText = validForm.storyText
		          ).withPublishedStatus(publishedStatus).saveWithImageAssets(image = productImageOption, icon = productIconOption)
              Redirect(controllers.routes.WebsiteControllers.getStorefrontChoosePhotoTiled(celebrity.urlSlug).url)
            }
          )
      	}
      }
    }
  }
  
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
  
  private def getUploadedImages(body: MultipartFormData[play.api.libs.Files.TemporaryFile])
  : (Option[File], Option[File], Option[BufferedImage], Option[BufferedImage]) = 
  {
    val productImageFile = body.file("productImage").map(_.ref.file).filterNot(_.length == 0)
    val productIconFile = body.file("productIcon").map(_.ref.file).filterNot(_.length == 0)
    val productImageOption = productImageFile.map(ImageUtil.parseImage(_)).flatten
    val productIconOption = productIconFile.map(ImageUtil.parseImage(_)).flatten
    (productImageFile, productIconFile, productImageOption, productIconOption)
  }

  /**
   * This is here so that demo'ers don't need to worry about setting up an InventoryBatch for demo Products before making orders.
   */
  private def maybeCreateInventoryBatchForDemoMode(product: Product, createWithoutInventory: String) {
    if (createWithoutInventory.isEmpty && !Play.isProd(Play.current)) {
      val dateFormat = new SimpleDateFormat("yyyy-MM-dd")
      val jan_01_2012 = dateFormat.parse("2012-01-01")
      val future = dateFormat.parse("2020-01-01")
      val inventoryBatch = InventoryBatch(celebrityId = product.celebrityId, numInventory = 100, startDate = jan_01_2012, endDate = future).save()
      inventoryBatch.products.associate(product)
    }
  }

  private def productNameIsUnique(celebrity: Celebrity, productId: Option[Long] = None): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      val productByUrlSlg = productStore.findByCelebrityAndUrlSlug(celebrity.id, Product.slugify(form.productName))
      val isCreate = (productId.isEmpty)
      val isUniqueUrlSlug = if (isCreate) {
        productByUrlSlg.isEmpty
      } else {
        productByUrlSlg.isEmpty || (productByUrlSlg.isDefined && productId == Some(productByUrlSlg.get.id))
      }
      if (isUniqueUrlSlug) Valid else Invalid("Celebrity already has a product with name: " + form.productName)
    }
  }

  private def productImageIsRequiredForCreate(productImageFile: Option[File]): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      if (productImageFile.isDefined) Valid else Invalid("Product image is required")
    }
  }

  private def productImageIsValid(productImageOption: Option[BufferedImage], productImageFile: Option[File], isCreate: Boolean): Constraint[PostProductForm] = {
    Constraint { form: PostProductForm =>
      val isProductImageValid = (isCreate && !productImageOption.isEmpty) || (!isCreate && (productImageFile.isEmpty || !productImageOption.isEmpty))
      if (isProductImageValid) {
        val validationResult = for (image <- productImageOption) yield {
	        val (width, height) = (image.getWidth, image.getHeight)
	        val resolutionStr = width + "x" + height
	        val isOriginalImageTooSmall = width < Product.minPhotoWidth || height < Product.minPhotoHeight
	        if (isOriginalImageTooSmall) {
	          Invalid("Product Photo must be at least " + Product.minPhotoWidth + " in width and " + Product.minPhotoHeight + " in height - resolution was " + resolutionStr)
	        } else if (width < height) {
            Invalid("Product Photo must be landscaped.")
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
