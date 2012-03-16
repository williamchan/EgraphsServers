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
import services.{Dimensions, ImageUtil}
import java.awt.image.BufferedImage
import models.{ImageAsset, ProductStore, CelebrityStore, Product}

trait PostCelebrityProductEndpoint {
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

    required("Product Photo", productImage)
    val dimensions: Option[Dimensions] = ImageUtil.getDimensions(productImage)
    if (dimensions.isEmpty) {
      Validation.addError("Product Image", "No image found for Product Image")
    } else {
      val resolutionStr = dimensions.get.width + ":" + dimensions.get.height
      Validation.isTrue("Product Image must be at least 900 in both width and height - resolution was " + resolutionStr, dimensions.get.width > 900 && dimensions.get.height > 900)
    }

    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductEndpoint.url(celebrity))
    }

    Logger.info("Creating product")
    val savedProduct = product.save()
    val cropDimensions = ImageUtil.getCropDimensions(dimensions.get)
    val croppedImage: BufferedImage = ImageUtil.crop(ImageIO.read(productImage), cropDimensions)
    // todo(wchan): Jpeg or PNG
    import services.ImageUtil.Conversions._
    savedProduct.withPhoto(croppedImage.asByteArray(ImageAsset.Jpeg)).save()

    new Redirect(GetCelebrityProductEndpoint.url(celebrity, savedProduct).url)
  }
}