package scenario

import utils.TestData
import services.blobs.Blobs
import models._
import models.enums.PublishedStatus
import play.api.Play
import play.api.Play.current
import services.AppConfig
import models.categories.Featured
import org.apache.commons.lang3.RandomStringUtils
import models.enums.OrderReviewStatus

/**
 * This object is meant to create scenarios similar to what is done on Scenarios, but
 * this will return the models needed so that you can use them.  No values that
 * can't be repeated should be used, this means no hardcoded ids and stuff like that.
 * These scenarios should allow our tests to be thread safe when using them.
 */
object RepeatableScenarios {

  def featured = AppConfig.instance[Featured]

  def createCelebrity(isFeatured: Boolean = false): Celebrity = {
    import Blobs.Conversions._
    val celebrity = TestData.newSavedCelebrity()

    celebrity.saveWithProfilePhoto(Play.getFile("test/resources/will_chan_celebrity_profile.jpg"))
    val (celeb, image) = celebrity.withLandingPageImage(Play.getFile("test/resources/ortiz_masthead.jpg"))
    image.save()
    celeb.save()


    if (isFeatured) {
      featured.updateFeaturedCelebrities(List(celebrity.id))
    }

    celebrity
  }

  /**
   * Adds products to this celebrity.  The first product is $100 each after is $100 more than the last.
   */
  def celebrityHasProducts(celebrity: Celebrity, numberOfProducts: Int = 2): Iterable[Product] = {
    val photoImage = Some(Product().defaultPhoto.renderFromMaster)
    val iconImage = Some(Product().defaultIcon.renderFromMaster)

    val products = for (i <- 1 to numberOfProducts) yield {
      TestData.newSavedProductWithoutInventoryBatch(Some(celebrity))
        .saveWithImageAssets(photoImage, iconImage)
        .withPrice(100 * i)
        .save()
    }

    val inventoryBatch = TestData.newSavedInventoryBatch(celebrity)

    for (product <- products) {
      inventoryBatch.products.associate(product)
    }

    products
  }

  /**
   * Customer buys every product the celebrity has available.
   */
  def customerBuysEveryProductOfCelebrity(customer: Customer, celebrity: Celebrity): Iterable[Order] = {
    val products = celebrity.productsInActiveInventoryBatches()
    for (product <- products) yield {
      val order = customer.buyUnsafe(
        product,
        recipientName = customer.name,
        requestedMessage = Some(RandomStringUtils.randomAlphabetic(100)),
        messageToCelebrity = Some(RandomStringUtils.randomAlphabetic(100)))
      order.save()
    }
  }

  def deliverOrdersToCelebrity(orders: Iterable[Order]): Iterable[Order] = {
    for (order <- orders) yield {
      order.copy(_reviewStatus = OrderReviewStatus.ApprovedByAdmin.name).save()
    }
  }
}