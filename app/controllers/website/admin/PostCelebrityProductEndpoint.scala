package controllers.website.admin

import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.mvc.Controller
import play.Logger
import services.blobs.Blobs.Conversions._
import java.io.File
import play.data.validation.Validation.required
import controllers.website.GetCelebrityProductEndpoint
import services.http.AdminRequestFilters
import services.http.OptionParams.Conversions._
import models.{ProductStore, CelebrityStore, Product}
import play.data.validation.Validation
import services.ImageUtil

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
    val dimensions = ImageUtil.getDimensions(productImage)
    if (dimensions.isEmpty) {
      Validation.addError("Product Image", "No image found for Product Image")
    } else {
      val aspectRatio: Double = dimensions.get.width.doubleValue() / dimensions.get.height
      val resolutionStr = dimensions.get.width + ":" + dimensions.get.height
      // TODO(wchan): revisit required dimensions. See https://egraphs.jira.com/wiki/display/DEV/Egraph+Page#EgraphPage-ImageSpecifications
      Validation.isTrue("Product Image must be of higher resolution - resolution was " + resolutionStr, dimensions.get.width > 700)
      Validation.isTrue("Product Image's aspect ratio must be greater - aspect ratio was " + resolutionStr, aspectRatio > 0.66)
    }

    if (!validationErrors.isEmpty) {
      return WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductEndpoint.url(celebrity))
    }

    Logger.info("Creating product")
    val savedProduct = product.save()
    savedProduct.withPhoto(productImage).save()

    new Redirect(GetCelebrityProductEndpoint.url(celebrity, savedProduct).url)
  }
}