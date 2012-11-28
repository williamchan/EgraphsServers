package models

import enums.PublishedStatus
import java.util.Date
import services.AppConfig
import utils._
import org.joda.time.DateTimeConstants

class GetCatalogStarsTests extends EgraphsUnitTest
  with DBTransactionPerTest
  with DateShouldMatchers
{
  private def celebrityStore = AppConfig.instance[CelebrityStore]

  "getCatalogStars" should "return only published and enrolled celebrities" in new EgraphsTestApplication {
    val publishedCelebrity1 = TestData.newSavedCelebrity()
    val publishedCelebrity2 = TestData.newSavedCelebrity()
    val unpublishedCelebrity1 = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()
    val unpublishedCelebrity2 = TestData.newSavedCelebrity().withPublishedStatus(PublishedStatus.Unpublished).save()

    TestData.newSavedProduct(celebrity = Some(publishedCelebrity1))
    TestData.newSavedProduct(celebrity = Some(publishedCelebrity2))
    TestData.newSavedProduct(celebrity = Some(unpublishedCelebrity1))
    TestData.newSavedProduct(celebrity = Some(unpublishedCelebrity2))

    val catalogStars = celebrityStore.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.map(star => star.name)
    celebrityNamesInCatalogStars should contain (publishedCelebrity1.publicName)
    celebrityNamesInCatalogStars should contain (publishedCelebrity2.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedCelebrity1.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedCelebrity2.publicName)
  }

  it should "return only celebrities with published products" in new EgraphsTestApplication {
    // create 4 different celebrities with products, 2 have unpublished products, all are published celebrities.
    val publishedProduct1 = TestData.newSavedProduct()
    val publishedProduct2 = TestData.newSavedProduct()
    val unpublishedProduct1 = TestData.newSavedProduct().withPublishedStatus(PublishedStatus.Unpublished).save()
    val unpublishedProduct2 = TestData.newSavedProduct().withPublishedStatus(PublishedStatus.Unpublished).save()

    val catalogStars = celebrityStore.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.map(star => star.name)
    celebrityNamesInCatalogStars should contain (publishedProduct1.celebrity.publicName)
    celebrityNamesInCatalogStars should contain (publishedProduct2.celebrity.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedProduct1.celebrity.publicName)
    celebrityNamesInCatalogStars should not contain (unpublishedProduct2.celebrity.publicName)
  }

  it should "return celebrities that lack inventory if they lack inventory batches active based on startDate and endDate" in new EgraphsTestApplication {
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

    val catalogStars = celebrityStore.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.map(star => (star.name, star.hasInventoryRemaining))
    celebrityNamesInCatalogStars should contain ((availableProduct1.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((availableProduct2.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((unavailableProduct1.celebrity.publicName, false))
    celebrityNamesInCatalogStars should contain ((unavailableProduct2.celebrity.publicName, false))
  }

  it should "show a celebrity has products available only if there is remaining inventory" in new EgraphsTestApplication {
    AppConfig.instance[services.cache.CacheFactory].applicationCache.clear()
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
    TestData.newSavedInventoryBatch(unavailableProduct3).copy(numInventory = 0).save()

    // we will also be testing when we have an inventoryBatch of quantity 0
    for (i <- 1 to startingInventory) {
      TestData.newSavedOrder(Some(unavailableProduct1))
      TestData.newSavedOrder(Some(unavailableProduct2))
    }

    val catalogStars = celebrityStore.getCatalogStars

    val celebrityNamesInCatalogStars = catalogStars.map(star => (star.name, star.hasInventoryRemaining))
    celebrityNamesInCatalogStars should contain ((availableProduct1.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((availableProduct2.celebrity.publicName, true))
    celebrityNamesInCatalogStars should contain ((unavailableProduct1.celebrity.publicName, false))
    celebrityNamesInCatalogStars should contain ((unavailableProduct2.celebrity.publicName, false))
    celebrityNamesInCatalogStars should contain ((unavailableProduct3.celebrity.publicName, false))
  }

  it should "show whether a celebrity has inventory based on current inventory batches" in new EgraphsTestApplication {
    // Celebrity with current inventory batch, and previous inventory batch with full orders
    val celebrity = TestData.newSavedCelebrity()
    val product = TestData.newSavedProductWithoutInventoryBatch(celebrity = Some(celebrity))

    // Create a completed used inventory batch that is in the past
    val pastIB = TestData.newSavedInventoryBatch(product).copy(numInventory = 5).save()
    for (i <- 0 until pastIB.numInventory) {
      TestData.newSavedOrder(Some(product))
    }
    pastIB.copy(startDate = TestData.jan_01_2012, endDate = TestData.feb_01_2012).save()

    // Create a current, partially used inventory batch
    TestData.newSavedInventoryBatch(product).copy(numInventory = 2).save()
    TestData.newSavedOrder(Some(product))

    celebrityStore.getCatalogStars.find(star => star.id == celebrity.id).get.hasInventoryRemaining should be(true)

    // Now making that current inventory batch fully used
    TestData.newSavedOrder(Some(product))
    celebrityStore.getCatalogStars.find(star => star.id == celebrity.id).get.hasInventoryRemaining should be(false)
  }
}