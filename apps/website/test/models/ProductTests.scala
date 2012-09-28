package models

import enums.{PublishedStatus, HasPublishedStatusTests}
import java.util.Date
import services.Time
import services.AppConfig
import utils._
import java.awt.image.BufferedImage
import org.joda.time.DateTimeConstants

class ProductTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Product]
  with CreatedUpdatedEntityTests[Long, Product]
  with DBTransactionPerTest
  with HasPublishedStatusTests[Product]
{
  val store = AppConfig.instance[ProductStore]

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

  "Product" should "require certain fields" in {
    var exception = intercept[IllegalArgumentException] {Product().save()}
    exception.getLocalizedMessage should include ("Product: name must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name").save()}
    exception.getLocalizedMessage should include ("Product: description must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name", description = "desc", storyTitle = "").save()}
    exception.getLocalizedMessage should include ("Product: storyTitle must be specified")
    exception = intercept[IllegalArgumentException] {Product(name = "name", description = "desc", storyText = "").save()}
    exception.getLocalizedMessage should include ("Product: storyText must be specified")
  }

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
    val iPadSigningPhotoUrl: String = product.photo.resizedWidth(product.signingScaleW).url
    rendered("id") should be(product.id)
    rendered("urlSlug") should be("Herp-Derp")
    rendered("photoUrl") should be(iPadSigningPhotoUrl)
    rendered("iPadSigningPhotoUrl") should be(iPadSigningPhotoUrl)
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
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProduct(celebrity = Some(celebrity)).copy(name = "Herp Derp").save()

    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = product.urlSlug) should not be (None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id + 1, slug = product.urlSlug) should be(None)
    store.findByCelebrityAndUrlSlug(celebrityId = celebrity.id, slug = "Herp") should be(None)
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

  "getCatalogStars" should "return only published celebrities" in {
    val publishedCelebrity1 = TestData.newSavedCelebrity()
    val unpublishedCelebrity1 = TestData.newSavedCelebrity()
    val publishedCelebrity2 = TestData.newSavedCelebrity()
    val unpublishedCelebrity2 = TestData.newSavedCelebrity()

    unpublishedCelebrity1.withPublishedStatus(PublishedStatus.Unpublished).save()
    unpublishedCelebrity2.withPublishedStatus(PublishedStatus.Unpublished).save()

    TestData.newSavedProduct(celebrity = Some(publishedCelebrity1))
    TestData.newSavedProduct(celebrity = Some(publishedCelebrity2))
    TestData.newSavedProduct(celebrity = Some(unpublishedCelebrity1))
    TestData.newSavedProduct(celebrity = Some(unpublishedCelebrity2))

    val catalogStars = store.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.map(star => star.name)
    celebrityNamesInCatalogStars should contain (publishedCelebrity1.publicName)
    celebrityNamesInCatalogStars should contain (publishedCelebrity2.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedCelebrity1.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedCelebrity2.publicName)
  }

  "getCatalogStars" should "return only celebrities with published products" in {
    // create 4 different celebrities with products, 2 have unpublished products, all are published celebrities.
    val publishedProduct1 = TestData.newSavedProduct()
    val unpublishedProduct1 = TestData.newSavedProduct().withPublishedStatus(PublishedStatus.Unpublished).save()
    val publishedProduct2 = TestData.newSavedProduct()
    val unpublishedProduct2 = TestData.newSavedProduct().withPublishedStatus(PublishedStatus.Unpublished).save()

    val catalogStars = store.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.filter(star => star.hasInventoryRemaining).map(star => star.name)
    celebrityNamesInCatalogStars should contain (publishedProduct1.celebrity.publicName)
    celebrityNamesInCatalogStars should contain (publishedProduct2.celebrity.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedProduct1.celebrity.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedProduct2.celebrity.publicName)
  }

  "getCatalogStars" should "return only celebrities that have an inventory batch that is still available by date" in {
    // create 4 different celebrities with products, 2 have unpublished products, all are published celebrities.
    val availableProduct1 = TestData.newSavedProduct()
    val availableProduct2 = TestData.newSavedProduct()

    val unavailableProduct1 = TestData.newSavedProductWithoutInventoryBatch()
    val unavailableProduct2 = TestData.newSavedProductWithoutInventoryBatch()

    // make sure the inventory is unavailable by date being in the past or future
    val hour = DateTimeConstants.MILLIS_PER_HOUR
    // expired half an hour ago.
    TestData.newSavedInventoryBatch(unavailableProduct1).copy(startDate = new Date(System.currentTimeMillis() - hour), endDate = new Date(System.currentTimeMillis() - hour/2)).save()
    // starts in half an hour
    TestData.newSavedInventoryBatch(unavailableProduct2).copy(startDate = new Date(System.currentTimeMillis() + hour/2), endDate = new Date(System.currentTimeMillis() + hour)).save()

    val catalogStars = store.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.filter(star => star.hasInventoryRemaining).map(star => (star.name, star.hasInventoryRemaining))
    celebrityNamesInCatalogStars should contain ((availableProduct1.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((availableProduct2.celebrity.publicName, true))
    celebrityNamesInCatalogStars should not contain ((unavailableProduct1.celebrity.publicName, false))
    celebrityNamesInCatalogStars should not contain ((unavailableProduct2.celebrity.publicName, false))
  }

  "getCatalogStars" should "show a celebrity has products available only if there is remaining inventory" in {
    // these two already have inventory by default
    val availableProduct1 = TestData.newSavedProduct()
    val availableProduct2 = TestData.newSavedProduct()

    // we need to create a specific number of inventory for these so we can create orders.
    val startingInventory = 10
    val unavailableProduct1 = TestData.newSavedProductWithoutInventoryBatch()
    val unavailableProduct2 = TestData.newSavedProductWithoutInventoryBatch()
    val unavailableProduct3 = TestData.newSavedProductWithoutInventoryBatch()
    TestData.newSavedInventoryBatch(unavailableProduct1).copy(numInventory = startingInventory).save()
    TestData.newSavedInventoryBatch(unavailableProduct2).copy(numInventory = startingInventory).save()
    TestData.newSavedInventoryBatch(unavailableProduct2).copy(numInventory = 0).save()

    // we will also be testing when we have an inventoryBatch of quantity 0
    for (i <- 1 to startingInventory) {
      TestData.newSavedOrder(Some(unavailableProduct1))
      TestData.newSavedOrder(Some(unavailableProduct2))
    }

    val catalogStars = store.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.filter(star => star.hasInventoryRemaining).map(star => (star.name, star.hasInventoryRemaining))
    celebrityNamesInCatalogStars should contain ((availableProduct1.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((availableProduct2.celebrity.publicName, true))
    celebrityNamesInCatalogStars should not contain ((unavailableProduct1.celebrity.publicName, false))
    celebrityNamesInCatalogStars should not contain ((unavailableProduct2.celebrity.publicName, false))
    celebrityNamesInCatalogStars should not contain ((unavailableProduct3.celebrity.publicName, false))
  }
}