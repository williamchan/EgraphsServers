package models

import org.scalatest.matchers.ShouldMatchers
import org.scalatest.BeforeAndAfterEach
import play.test.UnitFlatSpec
import services.Time
import services.AppConfig
import play.Play
import javax.imageio.ImageIO
import utils._
import java.awt.image.BufferedImage

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
    TestData.newSavedCelebrity().newProduct
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

  "saveWithImageAssets" should "set signingScaleH and signingScaleW" in {
    var product = TestData.newSavedProduct().saveWithImageAssets(image = Some(new BufferedImage(/*width*/3000, /*height*/2000, BufferedImage.TYPE_INT_ARGB)), icon = None)
    product.signingScaleW should be(Product.defaultLandscapeSigningScale.width)
    product.signingScaleH should be(Product.defaultLandscapeSigningScale.height)

    product = TestData.newSavedProduct().saveWithImageAssets(image = Some(new BufferedImage(/*width*/2000, /*height*/3000, BufferedImage.TYPE_INT_ARGB)), icon = None)
    product.signingScaleW should be(Product.defaultPortraitSigningScale.width)
    product.signingScaleH should be(Product.defaultPortraitSigningScale.height)
  }

  "renderedForApi" should "serialize the correct Map for the API" in {
    val product = TestData.newSavedProduct().copy(name = "Herp Derp", signingOriginX = 50, signingOriginY = 60).save()

    val rendered = product.renderedForApi
    rendered("id") should be(product.id)
    rendered("urlSlug") should be("Herp-Derp")
    rendered("photoUrl") should be(product.photo.resizedWidth(product.signingScaleW).url)
    rendered("signingScaleW") should be(Product.defaultLandscapeSigningScale.width)
    rendered("signingScaleH") should be(Product.defaultLandscapeSigningScale.height)
    rendered("signingOriginX") should be(50)
    rendered("signingOriginY") should be(60)
    rendered("signingAreaW") should be(Product.defaultSigningAreaW)
    rendered("signingAreaH") should be(Product.defaultSigningAreaW)
    rendered.contains("created") should be(true)
    rendered.contains("updated") should be(true)
  }

  "findByCelebrityAndUrlSlug" should "return Product with matching name and celebrityId" in {
    val celebrity = Celebrity().save()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity)).copy(name = "Herp Derp").save()

    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = product.urlSlug) should not be (None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id + 1, slug = product.urlSlug) should be(None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = "Herp") should be(None)
  }

  "A product's icon photo" should "start out as the default" in {
    import services.ImageUtil.Conversions._
    val product = TestData.newSavedProduct()
    val icon = TestData.newSavedProduct().icon
    icon.url should be (TestData.newSavedProduct().defaultIcon.url)

    val newIconImage = ImageIO.read(Play.getFile("public/images/egraph_default_plaque_icon.png"))
    val productWithIcon = product.withIcon(newIconImage.asByteArray(ImageAsset.Png)).save().product

    productWithIcon.iconUrl should not be (TestData.newSavedProduct().defaultIcon.url)
  }

  "getRemainingInventoryAndActiveInventoryBatches" should "return total inventory in active InventoryBatches minus the number of relevant Orders" in {
    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    product1.getRemainingInventoryAndActiveInventoryBatches() should be ((0, List.empty[InventoryBatch]))
    product2.getRemainingInventoryAndActiveInventoryBatches() should be ((0, List.empty[InventoryBatch]))

    val inventoryBatch1 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch2 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatch1.products.associate(product1)
    inventoryBatch1.products.associate(product2)
    inventoryBatch2.products.associate(product2)
    customer.buy(product1).copy(inventoryBatchId = inventoryBatch1.id).save()
    customer.buy(product2).copy(inventoryBatchId = inventoryBatch1.id).save()
    customer.buy(product2).copy(inventoryBatchId = inventoryBatch2.id).save()
    product1.getRemainingInventoryAndActiveInventoryBatches() should be ((48, List(inventoryBatch1)))                  // product1 is in inventoryBatch1, which has 2 purchases
    product2.getRemainingInventoryAndActiveInventoryBatches() should be ((97, List(inventoryBatch1, inventoryBatch2))) // product1 is in both inventoryBatch1 and inventoryBatch1, which have 3 purchases total
  }
}