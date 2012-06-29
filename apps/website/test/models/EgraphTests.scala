package models

import enums.EgraphState
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.blobs.Blobs.Conversions._
import services.{Dimensions, Time, AppConfig}
import java.awt.image.BufferedImage

class EgraphTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Egraph]
  with CreatedUpdatedEntityTests[Egraph]
  with ClearsDatabaseAndValidationBefore
  with DBTransactionPerTest
{
  private val store = AppConfig.instance[EgraphStore]
  private val egraphQueryFilters = AppConfig.instance[EgraphQueryFilters]

  //
  // SavingEntityTests[Egraph] methods
  //
  override def newEntity = {
    val order = TestData.newSavedOrder()
    Egraph(orderId=order.id)
  }

  override def saveEntity(toSave: Egraph) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Egraph) = {
    val order = TestData.newSavedOrder()
    toTransform.copy(orderId = order.id).withEgraphState(EgraphState.Published)
  }

  //
  // Test cases
  //
  "An Egraph" should "update its state when withEgraphState is called" in {
    val egraph = Egraph().withEgraphState(EgraphState.FailedBiometrics)

    egraph.egraphState should be (EgraphState.FailedBiometrics)
  }

  "approve" should "change state to ApprovedByAdmin" in {
    val admin = Administrator().save()
    Egraph().withEgraphState(EgraphState.PassedBiometrics).approve(admin).egraphState should be(EgraphState.ApprovedByAdmin)
    Egraph().withEgraphState(EgraphState.FailedBiometrics).approve(admin).egraphState should be(EgraphState.ApprovedByAdmin)
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.PassedBiometrics).approve(null)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.AwaitingVerification).approve(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.Published).approve(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.RejectedByAdmin).approve(admin)}
  }

  "reject" should "change state to RejectedByAdmin" in {
    val admin = Administrator().save()
    Egraph().withEgraphState(EgraphState.PassedBiometrics).reject(admin).egraphState should be(EgraphState.RejectedByAdmin)
    Egraph().withEgraphState(EgraphState.FailedBiometrics).reject(admin).egraphState should be(EgraphState.RejectedByAdmin)
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.PassedBiometrics).reject(null)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.AwaitingVerification).reject(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.Published).reject(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.RejectedByAdmin).reject(admin)}
  }

  "publish" should "change state to Published" in {
    val admin = Administrator().save()
    Egraph().withEgraphState(EgraphState.ApprovedByAdmin).publish(admin).egraphState should be(EgraphState.Published)
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.PassedBiometrics).publish(null)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.AwaitingVerification).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.RejectedByAdmin).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.PassedBiometrics).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withEgraphState(EgraphState.PassedBiometrics).publish(admin)}
  }

  "getSignedAt" should "return signedAt timestamp if it exists, otherwise created" in {
    var egraph = TestData.newSavedEgraph().copy(signedAt = Time.timestamp("2012-07-12 15:11:22.987", Time.ipadDateFormat)).save()
    egraph.signedAt should not be(None)
    egraph.signedAt should not be(Some(egraph.created))

    egraph = egraph.copy(signedAt = None).save()
    egraph.signedAt should be(None)
  }

  "image" should "return EgraphImage with correctly configured ingredientFactory" in {
    val egraph = newEntity.withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.fakeAudioStr()).save()
    val egraphImage: EgraphImage = egraph.image()
    val ingredientFactory = egraphImage.ingredientFactory.apply() // This throws NPEs if signature, message, pen, or photo are uninitialized
    ingredientFactory.photoDimensionsWhenSigned.width should be(Product.defaultLandscapeSigningScale.width)
    ingredientFactory.photoDimensionsWhenSigned.height should be(Product.defaultLandscapeSigningScale.height)
    ingredientFactory.signingOriginX should be(0)
    ingredientFactory.signingOriginY should be(0)
  }

  "An Egraph" should "save and recover signature and audio data from the blobstore" in {
    val egraph = TestData.newSavedOrder()
      .newEgraph
      .withAssets(TestConstants.shortWritingStr, Some(TestConstants.shortWritingStr), TestConstants.fakeAudio)
      .save()

    egraph.assets.signature should be (TestConstants.shortWritingStr)
    egraph.assets.audioWav.asByteArray should be (TestConstants.fakeAudio)
  }

  "generateAndSaveMp3" should "store mp3 asset" in {
    val egraph = TestData.newSavedEgraphWithRealAudio()
    intercept[NoSuchElementException] { egraph.assets.audioMp3 }
    egraph.assets.generateAndSaveMp3()
    egraph.assets.audioMp3.asByteArray.length should be > (0)
  }

  "audioMp3Url" should "lazily create mp3 asset" in {
    val egraph = TestData.newSavedEgraphWithRealAudio()
    intercept[NoSuchElementException] { egraph.assets.audioMp3 }
    egraph.assets.audioMp3Url.endsWith("audio.mp3") should be(true)
    egraph.assets.audioMp3.asByteArray.length should be > (0)
  }

  "An Egraph" should "throw an exception if assets are accessed on an unsaved Egraph" in {
    evaluating { TestData.newSavedOrder().newEgraph.assets } should produce [IllegalArgumentException]
  }

  "An Egraph Story" should "render all values correctly in the title" in {
    val storyTemplate = EgraphStoryField.values.foldLeft("") { (accum, field) =>
      accum + "{" + field.name + "}"
    }
    val story = EgraphStory(
      titleTemplate = "{signer_name}",
      bodyTemplate = storyTemplate,
      celebName = "Herpy Derpson",
      celebUrlSlug = "Herpy-Derpson",
      recipientName = "Erem Recipient",
      productName = "NBA Finals 2012",
      productUrlSlug = "NBA-Finals-2012",
      orderTimestamp = Time.fromApiFormat("2012-02-10 13:00:00.256"),
      signingTimestamp = Time.fromApiFormat("2011-02-10 13:00:00.256")
    )
    story.title should be ("Herpy Derpson")
    story.body should be ("Herpy Derpson<a href='/Herpy-Derpson' >Erem RecipientNBA Finals 2012<a href='/Herpy-Derpson/NBA-Finals-2012' >February 10, 2012February 10, 2011</a>")
  }

  "getEgraphsAndResults" should "filter queries based on EgraphQueryFilters" in {
    val passedBiometrics = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.PassedBiometrics).save()
    val failedBiometrics = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.FailedBiometrics).save()
    val approvedByAdmin = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val rejectedByAdmin = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.RejectedByAdmin).save()
    val awaitingVerification = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.AwaitingVerification).save()
    val published = TestData.newSavedOrder().newEgraph.withEgraphState(EgraphState.Published).save()

    store.getEgraphsAndResults(egraphQueryFilters.passedBiometrics).toSeq.map(e => e._1).toSet should be(Set(passedBiometrics))
    store.getEgraphsAndResults(egraphQueryFilters.failedBiometrics).toSeq.map(e => e._1).toSet should be(Set(failedBiometrics))
    store.getEgraphsAndResults(egraphQueryFilters.approvedByAdmin).toSeq.map(e => e._1).toSet should be(Set(approvedByAdmin))
    store.getEgraphsAndResults(egraphQueryFilters.rejectedByAdmin).toSeq.map(e => e._1).toSet should be(Set(rejectedByAdmin))
    store.getEgraphsAndResults(egraphQueryFilters.awaitingVerification).toSeq.map(e => e._1).toSet should be(Set(awaitingVerification))
    store.getEgraphsAndResults(egraphQueryFilters.published).toSeq.map(e => e._1).toSet should be(Set(published))
    store.getEgraphsAndResults(egraphQueryFilters.pendingAdminReview).toSeq.map(e => e._1).toSet should be(Set(passedBiometrics, failedBiometrics, approvedByAdmin))
  }

  "getCelebrityEgraphsAndResults" should "filter queries based on EgraphQueryFilters" in {
    val celebrity = TestData.newSavedCelebrity()
    val product = Some(TestData.newSavedProduct(Some(celebrity)))

    val passedBiometrics = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.PassedBiometrics).save()
    val failedBiometrics = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.FailedBiometrics).save()
    val approvedByAdmin = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val rejectedByAdmin = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.RejectedByAdmin).save()
    val awaitingVerification = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.AwaitingVerification).save()
    val published = TestData.newSavedOrder(product).newEgraph.withEgraphState(EgraphState.Published).save()

    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.passedBiometrics).toSeq.map(e => e._1).toSet should be(Set(passedBiometrics))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.failedBiometrics).toSeq.map(e => e._1).toSet should be(Set(failedBiometrics))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.approvedByAdmin).toSeq.map(e => e._1).toSet should be(Set(approvedByAdmin))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.rejectedByAdmin).toSeq.map(e => e._1).toSet should be(Set(rejectedByAdmin))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.awaitingVerification).toSeq.map(e => e._1).toSet should be(Set(awaitingVerification))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.published).toSeq.map(e => e._1).toSet should be(Set(published))
    store.getCelebrityEgraphsAndResults(celebrity, egraphQueryFilters.pendingAdminReview).toSeq.map(e => e._1).toSet should be(Set(passedBiometrics, failedBiometrics, approvedByAdmin))
  }
}

class EgraphFrameTests extends EgraphsUnitTest {
  "An Egraph Frame" should "select the correct frame for landscape image dimensions" in {
    EgraphFrame.suggestedFrame(Dimensions(501,500)) should be (LandscapeEgraphFrame)
    EgraphFrame.suggestedFrame(Dimensions(500,501)) should be (PortraitEgraphFrame)
    EgraphFrame.suggestedFrame(Dimensions(500,500)) should be (PortraitEgraphFrame)
  }

  it should "report the correct aspect ratio" in {
    PortraitEgraphFrame.imageAspectRatio should be (.7167)
    LandscapeEgraphFrame.imageAspectRatio should be (1.5782)
  }

  it should "not crop when dimensions are already ideal for landscape (width > height)" in {
    val uncropped = new BufferedImage(1578, 1000, BufferedImage.TYPE_INT_ARGB)
    val cropped = LandscapeEgraphFrame.cropImageForFrame(uncropped)

    (cropped.getWidth, cropped.getHeight) should be ((uncropped.getWidth, uncropped.getHeight - 1))
  }

  it should "not crop when dimensions are already ideal for portrait" in {
    val uncropped = new BufferedImage(717, 1000, BufferedImage.TYPE_INT_ARGB)
    val cropped = PortraitEgraphFrame.cropImageForFrame(uncropped)

    (cropped.getWidth, cropped.getHeight) should be ((uncropped.getWidth - 1, uncropped.getHeight))
  }

  it should "crop to correct aspect ratio when too wide" in {
    val uncropped = new BufferedImage(717, 2000, BufferedImage.TYPE_INT_ARGB)
    val cropped = PortraitEgraphFrame.cropImageForFrame(uncropped)

    (cropped.getWidth, cropped.getHeight) should be ((717, 1000))
  }

  it should "crop to correct aspect ratio when too tall" in {
    val uncropped = new BufferedImage(377, 1000, BufferedImage.TYPE_INT_ARGB)
    val cropped = PortraitEgraphFrame.cropImageForFrame(uncropped)

    (cropped.getWidth, cropped.getHeight) should be ((377, 526))
  }
}