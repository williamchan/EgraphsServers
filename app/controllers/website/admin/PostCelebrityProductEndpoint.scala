package controllers.website.admin

import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.mvc.Controller
import play.Logger
import java.io.File
import play.data.validation.Validation.required
import controllers.website.GetCelebrityProductEndpoint
import services.http.AdminRequestFilters
import services.http.OptionParams.Conversions._
import play.data.validation.Validation
import javax.imageio.ImageIO
import models._
import services.ImageUtil.Conversions._
import services.{Logging, Dimensions, ImageUtil}

trait PostCelebrityProductEndpoint extends Logging {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  def postCelebrityProduct(productName: String,
                           productDescription: String,
                           productImage: File): Redirect = {

    val celebrityId = request.params.getOption("celebrityId").get.toLong
    val celebrity = celebrityStore.findById(celebrityId).get
    val product = celebrity.newProduct.copy(
      priceInCurrency = Product.defaultPrice,
      name = productName,
      description = productDescription
    )

    required("Product name", productName)
    if (productName.isEmpty) {
      Validation.isTrue("Celebrity already has a product with name: " + productName, productStore.findByCelebrityAndUrlSlug(celebrityId = celebrityId, slug = product.urlSlug).isEmpty)
    }

    val dimensionsOption: Option[Dimensions] = ImageUtil.getDimensions(productImage)
    if (dimensionsOption.isEmpty) {
      Validation.addError("Product Photo", "No image found for Product Image")
    } else {
      val resolutionStr = dimensionsOption.get.width + "x" + dimensionsOption.get.height
      Validation.isTrue(
        "Product Photo must be at least 940 in width and 900 in height - resolution was " + resolutionStr,
         dimensionsOption.get.width >= 940 && dimensionsOption.get.height >= 900
      )
    }

    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductEndpoint.url(celebrity))
    }

    log("Creating product \"" + productName + "\" for celebrity \"" + celebrity.publicName + "\"");
    val dimensions = dimensionsOption.get
    val frame = EgraphFrame.suggestedFrame(dimensions)
    val uploadedImage = ImageIO.read(productImage)
    val imageCroppedToFrame = frame.cropImageForFrame(uploadedImage)
    // todo(wchan): Jpeg or PNG
    val imageByteArray = imageCroppedToFrame.asByteArray(ImageAsset.Jpeg)
    val savedProduct = product
      .withFrame(frame)
      .save()
      .withPhoto(imageByteArray)
      .save()
      .product

    new Redirect(GetCelebrityProductEndpoint.url(celebrity, savedProduct).url)
  }
}