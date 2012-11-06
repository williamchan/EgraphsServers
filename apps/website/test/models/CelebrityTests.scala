package models

import enums.{HasPublishedStatusTests, PublishedStatus}
import javax.imageio.ImageIO
import services.Time
import services.ImageUtil.Conversions._
import services.AppConfig
import play.api.Play
import utils._

class CelebrityTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Celebrity]
  with CreatedUpdatedEntityTests[Long, Celebrity]
  with DateShouldMatchers
  with DBTransactionPerTest
  with HasPublishedStatusTests[Celebrity]
{
  def store = AppConfig.instance[CelebrityStore]
  
  //
  // Test cases
  //
  "A Celebrity" should "render to API format properly" in new EgraphsTestApplication {
    val publicName = TestData.generateFullname()
    val celeb = Celebrity(publicName = publicName).save()
    val apiMap = celeb.renderedForApi

    apiMap("publicName") should be (publicName)
    apiMap("urlSlug") should be (publicName.replaceAll(" ", "-"))
    apiMap("id") should be (celeb.id)
    apiMap("created") should be (Time.toApiFormat(celeb.created))
    apiMap("updated") should be (Time.toApiFormat(celeb.updated))
  }

  it should "start with the default profile photo" in new EgraphsTestApplication {
    val celebrity = Celebrity()
    celebrity.profilePhotoUpdated should be (None)
    celebrity.profilePhoto should be (celebrity.defaultProfile)
  }

  it should "throw an exception if you save profile photo when id is 0" in new EgraphsTestApplication {
    val celeb = newEntity
    val image = ImageIO.read(resourceFile("image.jpg"))

    evaluating { celeb.saveWithProfilePhoto(image.asByteArray(ImageAsset.Png)) } should produce [IllegalArgumentException]
  }

  it should "store and retrieve the profile image asset" in new EgraphsTestApplication {
    val celeb = TestData.newSavedCelebrity()
    val image = ImageIO.read(resourceFile("image.jpg"))

    val (savedCeleb, imageAsset) = celeb.save().saveWithProfilePhoto(image.asByteArray(ImageAsset.Png))

    imageAsset.key should include ("celebrity/" + celeb.id)
    savedCeleb.profilePhotoUpdated.get.toLong should be (Time.toBlobstoreFormat(Time.now).toLong plusOrMinus 10000)
    savedCeleb.profilePhoto should not be (None)

    val profilePhoto = savedCeleb.profilePhoto
    profilePhoto.renderFromMaster.asByteArray(ImageAsset.Png).length should be (imageAsset.renderFromMaster.asByteArray(ImageAsset.Png).length)
  }

  "productsInActiveInventoryBatches" should "return Products associated with active InventoryBatches" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    celebrity.productsInActiveInventoryBatches().length should be(0)

    val product1 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    val product2 = TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity)
    TestData.newSavedProductWithoutInventoryBatch(celebrity = celebrity) // not used
    val inventoryBatch1 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    val inventoryBatch2 = TestData.newSavedInventoryBatch(celebrity = celebrity)
    inventoryBatch1.products.associate(product1)
    inventoryBatch2.products.associate(product1)
    inventoryBatch2.products.associate(product2)
    celebrity.productsInActiveInventoryBatches().toSet should be(Set(product1, product2))
  }

  "getFeaturedPublishedCelebrities" should "only return published celebrities that are featured" in new EgraphsTestApplication {
    import PublishedStatus.{Published, Unpublished}

    // Set up
    val featuredPublishedShouldBeInResults = Vector(
      (true, Published, true),
      (true, Unpublished, false),
      (false, Published, false),
      (false, Unpublished, false)
    )

    val celebs = for ((featured, published, _) <- featuredPublishedShouldBeInResults) yield {
      TestData.newSavedCelebrity()
        .copy(isFeatured=featured)
        .withPublishedStatus(published)
        .save()
    }

    val results = store.getFeaturedPublishedCelebrities.toList

    // Execute the test on the data table featuredPublishedShouldBeInResults
    for ((celeb, i) <- celebs.zipWithIndex) {
      val shouldBeInResults = featuredPublishedShouldBeInResults(i)._3
      if (shouldBeInResults) results should contain (celeb) else results should not contain (celeb)
    }
  }

  "updateFeaturedCelebrities" should "remove celebs that weren't in the updated featured list" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured=true, includeCelebInNewFeaturedCelebs=false) should be (false)
  }

  "updateFeaturedCelebrities" should "keep featured celebs" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured=true, includeCelebInNewFeaturedCelebs=true) should be (true)
  }

  "updateFeaturedCelebrities" should "set newly featured celebs" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured=false, includeCelebInNewFeaturedCelebs=true) should be (true)
  }
  
  "find pairs of categories and CategoryValues" should "return the pairs associated with the celeb" in {
    val categoryA = TestData.newSavedCategory
    val categoryValueA = TestData.newSavedCategoryValue(categoryA.id)
    
    val categoryB = TestData.newSavedCategory
    val categoryValueB = TestData.newSavedCategoryValue(categoryB.id)
    
    val celeb = TestData.newSavedCelebrity().save()
    celeb.categoryValues.associate(categoryValueA)
    celeb.categoryValues.associate(categoryValueB)
    val pairs = celeb.categoryValueAndCategoryPairs
    
    pairs.size should be(2)
  }
  

  //
  //  HasPublishedStatus[Celebrity] methods
  //
  override def newPublishableEntity = {
    Celebrity(publicName = TestData.generateFullname())
  }

  //
  // SavingEntityTests[Celebrity] methods
  //
  override def newEntity = {
    Celebrity(publicName = TestData.generateFullname())
  }

  override def saveEntity(toSave: Celebrity) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Celebrity) = {
    toTransform.copy(
      apiKey = Some("apiKey"),
      publicName = TestData.generateFullname(),
      isFeatured = true,
      roleDescription = "Pitcher, Tampa Bay Rays",
      profilePhotoUpdated = Some(Time.toBlobstoreFormat(Time.now))
    ).withPublishedStatus(PublishedStatus.Published)
  }

  it should "return all associated CategoryValues" in {
    val celeb = TestData.newSavedCelebrity()
    val category1 = TestData.newSavedCategory
    val category2 = TestData.newSavedCategory
    val categoryValue1 = TestData.newSavedCategoryValue(category1.id)
    val categoryValue2 = TestData.newSavedCategoryValue(category2.id)

    celeb.categoryValues.associate(categoryValue1)
    celeb.categoryValues.associate(categoryValue2)

    celeb.categoryValues.size should be (2)
    celeb.categoryValues.exists(cv => cv.id == categoryValue1.id) should be (true)
    celeb.categoryValues.exists(cv => cv.id == categoryValue2.id) should be (true)
  }

  //
  // Private methods
  //

  private def featuredStateOfCelebWhen(
    celebWasFeatured: Boolean,
    includeCelebInNewFeaturedCelebs: Boolean): Boolean =
  {
    val celeb = TestData.newSavedCelebrity().copy(isFeatured=celebWasFeatured).save()
    val newFeaturedCelebs = if(includeCelebInNewFeaturedCelebs) {
      List(celeb.id)
    } else {
      List(0L) // a list of celebrity ids that can't be this celebrity
    }

    store.updateFeaturedCelebrities(newFeaturedCelebs)
    store.get(celeb.id).isFeatured
  }

}
