package models

import java.util.Date
import java.awt.image.BufferedImage
import org.joda.time.DateTimeConstants
import play.api.Play
import play.api.libs.json.Json
import services.Time
import services.AppConfig
import utils._
import enums.{PublishedStatus, HasPublishedStatusTests}

class ProductTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Product]
  with CreatedUpdatedEntityTests[Long, Product]
  with DBTransactionPerTest
  with DateShouldMatchers
  with HasPublishedStatusTests[Product]
{
  private def store = AppConfig.instance[ProductStore]

  //
  // HasPublishedStatusTests[Product]
  //
  override def newPublishableEntity = {
    Product()
  }

  //
  // SavingEntityTests[Product] methods
  //
  override def newEntity = {
    TestData.newSavedCelebrity().newProduct.copy(name = "prod", description = "desc")
  }

  override def saveEntity(toSave: Product) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Product) = {
    toTransform.copy(
      priceInCurrency = 1000,
      name = "NBA Championships 2010",
      photoKey = Some(Time.toBlobstoreFormat(Time.now)),
      description = "Shaq goes for the final dunk in the championship",
      _defaultFrameName = PortraitEgraphFrame.name,
      storyTitle = "He herped then he derped.",
      storyText = "He derped then he herped."
    )
  }

  //
  // Test cases
  //
  "Product" should "require certain fields" in new EgraphsTestApplication {
    var exception = intercept[IllegalArgumentException] {Product().save()}
    exception.getLocalizedMessage should include ("Product: name must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name").save()}
    exception.getLocalizedMessage should include ("Product: description must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name", description = "desc", storyTitle = "").save()}
    exception.getLocalizedMessage should include ("Product: storyTitle must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name", description = "desc", storyText = "").save()}
    exception.getLocalizedMessage should include ("Product: storyText must be specified")
  }

  "saveWithImageAssets" should "set signingScaleH and signingScaleW" in new EgraphsTestApplication {
    var product = TestData.newSavedProduct().saveWithImageAssets(image = Some(new BufferedImage(/*width*/3000, /*height*/2000, BufferedImage.TYPE_INT_ARGB)), icon = None)
    product.signingScaleW should be(Product.defaultLandscapeSigningScale.width)
    product.signingScaleH should be(Product.defaultLandscapeSigningScale.height)

    product = TestData.newSavedProduct().saveWithImageAssets(image = Some(new BufferedImage(/*width*/2000, /*height*/3000, BufferedImage.TYPE_INT_ARGB)), icon = None)
    product.signingScaleW should be(Product.defaultPortraitSigningScale.width)
    product.signingScaleH should be(Product.defaultPortraitSigningScale.height)
  }

  "urlSlug" should "slugify the name" in new EgraphsTestApplication {
    val product = TestData.newSavedProduct().copy(name = "Herp Derp").save()
    product.urlSlug should be ("Herp-Derp")
  }

  "toJson" should "serialize the correctly for the API" in new EgraphsTestApplication {
    val product = TestData.newSavedProduct().copy(name = "Herp Derp", signingOriginX = 50, signingOriginY = 60).save()

    val json = Json.toJson(JsProduct.from(product))
    val productFromJson = json.as[JsProduct]

    productFromJson should be (JsProduct.from(product))
  }

  "findByCelebrityAndUrlSlug" should "return Product with matching name and celebrityId" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity)).copy(name = "Herp Derp").save()

    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = product.urlSlug) should not be (None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id + 1, slug = product.urlSlug) should be(None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = "Herp") should be(None)
  }
}
