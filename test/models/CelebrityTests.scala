package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import javax.imageio.ImageIO
import services.Time
import services.ImageUtil.Conversions._
import services.AppConfig
import play.Play
import utils._

class CelebrityTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Celebrity]
  with CreatedUpdatedEntityTests[Celebrity]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
  with HasPublishedStatusTests[Celebrity]
{
  val store = AppConfig.instance[CelebrityStore]

  //
  //  HasPublishedStatus[Celebrity] methods
  //
  override def newPublishableEntity = {
    Celebrity()
  }

  //
  // SavingEntityTests[Celebrity] methods
  //
  override def newEntity = {
    Celebrity()
  }

  override def saveEntity(toSave: Celebrity) = {
    store.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Celebrity) = {
    toTransform.copy(
      apiKey = Some("apiKey"),
      description = Some("desc"),
      publicName = Some("pname"),
      isFeatured = true,
      roleDescription = Some("Pitcher, Tampa Bay Rays"),
      profilePhotoUpdated = Some(Time.toBlobstoreFormat(Time.now))
    ).withPublishedStatus(PublishedStatus.Published)
  }

  //
  // Test cases
  //
  "A Celebrity" should "render to API format properly" in {
    val celeb = Celebrity(
      firstName = Some("Will"),
      lastName = Some("Chan"),
      publicName = Some("Wizzle Chan")
    ).save()

    val apiMap = celeb.renderedForApi

    apiMap("firstName") should be ("Will")
    apiMap("lastName") should be ("Chan")
    apiMap("publicName") should be ("Wizzle Chan")
    apiMap("urlSlug") should be ("Wizzle-Chan")
    Some(apiMap("isLeftHanded")) should be (Some(false))
    apiMap("id") should be (celeb.id)
    apiMap("created") should be (Time.toApiFormat(celeb.created))
    apiMap("updated") should be (Time.toApiFormat(celeb.updated))
  }

  it should "start with the default profile photo" in {
    val celebrity = Celebrity()
    celebrity.profilePhotoUpdated should be (None)
    celebrity.profilePhoto should be (celebrity.defaultProfile)
  }

  it should "throw an exception if you save profile photo when id is 0" in {
    val celeb = newEntity
    val image = ImageIO.read(Play.getFile("test/files/image.png"))

    evaluating { celeb.saveWithProfilePhoto(image.asByteArray(ImageAsset.Png)) } should produce [IllegalArgumentException]
  }

  it should "store and retrieve the profile image asset" in {
    val celeb = TestData.newSavedCelebrity()
    val image = ImageIO.read(Play.getFile("test/files/image.png"))

    val (savedCeleb, imageAsset) = celeb.save().saveWithProfilePhoto(image.asByteArray(ImageAsset.Png))

    imageAsset.key should include ("celebrity/1")
    savedCeleb.profilePhotoUpdated.get.toLong should be (Time.toBlobstoreFormat(Time.now).toLong plusOrMinus 10000)
    savedCeleb.profilePhoto should not be (None)

    val profilePhoto = savedCeleb.profilePhoto
    profilePhoto.renderFromMaster.asByteArray(ImageAsset.Png).length should be (imageAsset.renderFromMaster.asByteArray(ImageAsset.Png).length)
  }

  "getActiveProducts" should "return Products associated with active InventoryBatches" in {
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

  "getFeaturedPublishedCelebrities" should "only return published celebrities that are featured" in {
    import PublishedStatus.{Published, Unpublished}

    // Set up
    val featuredPublishedShouldBeInResults = Vector(
      (true, Published, true),
      (true, Unpublished, false),
      (false, Published, false),
      (false, Unpublished, false)
    )

    val celebs = for (val (featured, published, _) <- featuredPublishedShouldBeInResults) yield {
      TestData.newSavedCelebrity()
        .copy(isFeatured=featured)
        .withPublishedStatus(published)
        .save()
    }

    val results = store.getFeaturedPublishedCelebrities.toList

    // Execute the test on the data table featuredPublishedShouldBeInResults
    for ( val (celeb, i) <- celebs.zipWithIndex) {
      val shouldBeInResults = featuredPublishedShouldBeInResults(i)._3
      if (shouldBeInResults) results should contain (celeb) else results should not contain (celeb)
    }
  }

  def newCelebWithFeatureAndPublishedStatus(isFeatured:Boolean, published:PublishedStatus.EnumVal) = {

  }
}