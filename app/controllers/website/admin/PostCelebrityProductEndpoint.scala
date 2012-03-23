package controllers.website.admin

import play.mvc.results.Redirect
import controllers.WebsiteControllers
import play.mvc.Controller
import java.io.File
import play.data.validation.Validation.required
import controllers.website.GetCelebrityProductEndpoint
import play.data.validation.Validation
import models._
import services.{Logging, ImageUtil}
import services.http.{CelebrityAccountRequestFilters, AdminRequestFilters}

trait PostCelebrityProductEndpoint extends Logging {
  this: Controller =>

  protected def adminFilters: AdminRequestFilters
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def celebrityStore: CelebrityStore
  protected def productStore: ProductStore

  def postCelebrityProduct(productName: String,
                           productDescription: String,
                           productImage: File,
                           productIcon: File,
                           storyTitle: String,
                           storyText: String) = {
    celebFilters.requireCelebrityId(request) { celebrity =>
      // Validate text fields
      required("Product name", productName)
      required("Product image", productImage)
      required("Product icon", productIcon)
      required("Story title", storyTitle)
      required("Story text", storyText)
      Validation.isTrue(
        "Celebrity already has a product with name: " + productName,
        productStore.findByCelebrityAndUrlSlug(celebrity.id, Product.slugify(productName)).isEmpty
      )
      
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
        val savedProduct = celebrity.addProduct(
          name=productName,
          description=productDescription,
          image=productImageOption.get,
          icon=productIconOption.get,
          storyTitle=storyTitle,
          storyText=storyText
        )

        new Redirect(GetCelebrityProductEndpoint.url(celebrity, savedProduct).url)
      }
      else {
        // There were validation errors
        WebsiteControllers.redirectWithValidationErrors(GetCreateCelebrityProductEndpoint.url(celebrity))
      }
    }
  }
}