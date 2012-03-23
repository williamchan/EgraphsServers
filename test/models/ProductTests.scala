package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import services.Time
import services.AppConfig
import play.Play
import javax.imageio.ImageIO

class ProductTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Product]
  with CreatedUpdatedEntityTests[Product]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val store = AppConfig.instance[ProductStore]

  //
  // SavingEntityTests[Product] methods
  //
  override def newEntity = {
    Celebrity().save().newProduct
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
  "A product" should "serialize the correct Map for the API" in {
    val product = Celebrity().save().newProduct.copy(name = "Herp Derp").save()

    val rendered = product.renderedForApi
    rendered("id") should be(product.id)
    rendered("photoUrl") should be(product.photo.url)
    rendered("urlSlug") should be("Herp-Derp")
    rendered.contains("created") should be(true)
    rendered.contains("updated") should be(true)
  }

  "findByCelebrityAndUrlSlug" should "return Product with matching name and celebrityId" in {
    val celebrity = Celebrity().save()
    val product = celebrity.newProduct.copy(name = "Herp Derp").save()

    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = product.urlSlug) should not be (None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id + 1, slug = product.urlSlug) should be(None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = "Herp") should be(None)

  }
  
  "A product's icon photo" should "start out as the default" in {
    import services.ImageUtil.Conversions._
    val product = newProduct
    val icon = newProduct.icon
    icon.url should be (newProduct.defaultIcon.url)

    val newIconImage = ImageIO.read(Play.getFile("public/images/egraph_default_plaque_icon.png"))
    val productWithIcon = product.withIcon(newIconImage.asByteArray(ImageAsset.Png)).save().product

    productWithIcon.iconUrl should not be (newProduct.defaultIcon.url)
  }
  
  def newProduct: Product = {
    Celebrity().save().newProduct.copy(name = "Herp Derp").save()
  }

}