package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils._
import services.blobs.Blobs.Conversions._
import services.{Dimensions, Time, AppConfig}
import java.awt.image.BufferedImage
import models.Egraph.EgraphState

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
    toTransform.copy(
      orderId = order.id,
      stateValue = EgraphState.Published.value
    )
  }

  //
  // Test cases
  //
  "An Egraph" should "update its state when withState is called" in {
    val egraph = Egraph().withState(EgraphState.FailedBiometrics)

    egraph.state should be (EgraphState.FailedBiometrics)
  }

  "approve" should "change state to ApprovedByAdmin" in {
    val admin = Administrator().save()
    Egraph().withState(EgraphState.PassedBiometrics).approve(admin).state should be(EgraphState.ApprovedByAdmin)
    Egraph().withState(EgraphState.FailedBiometrics).approve(admin).state should be(EgraphState.ApprovedByAdmin)
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.PassedBiometrics).approve(null)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.AwaitingVerification).approve(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.Published).approve(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.RejectedByAdmin).approve(admin)}
  }

  "reject" should "change state to RejectedByAdmin" in {
    val admin = Administrator().save()
    Egraph().withState(EgraphState.PassedBiometrics).reject(admin).state should be(EgraphState.RejectedByAdmin)
    Egraph().withState(EgraphState.FailedBiometrics).reject(admin).state should be(EgraphState.RejectedByAdmin)
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.PassedBiometrics).reject(null)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.AwaitingVerification).reject(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.Published).reject(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.RejectedByAdmin).reject(admin)}
  }

  "publish" should "change state to Published" in {
    val admin = Administrator().save()
    Egraph().withState(EgraphState.ApprovedByAdmin).publish(admin).state should be(EgraphState.Published)
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.PassedBiometrics).publish(null)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.AwaitingVerification).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.RejectedByAdmin).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.PassedBiometrics).publish(admin)}
    intercept[IllegalArgumentException] {Egraph().withState(EgraphState.PassedBiometrics).publish(admin)}
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
    egraph.assets.audio.asByteArray should be (TestConstants.fakeAudio)
  }

  "An Egraph" should "throw an exception if assets are accessed on an unsaved Egraph" in {
    evaluating { TestData.newSavedOrder().newEgraph.assets } should produce [IllegalArgumentException]
  }

  "Egraph statuses" should "be accessible via their values on the companion object" in {
    EgraphState.all.foreach( stateTuple =>
      EgraphState.all(stateTuple._2.value) should be (stateTuple._2)
    )
  }

  "allStatuses" should "contain all the states" in {
    EgraphState.all.size should be (6)
  }

  it should "throw an exception at an unrecognized string" in {
    evaluating { EgraphState.all("Herpyderp") } should produce [NoSuchElementException]
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
    val passedBiometrics = TestData.newSavedOrder().newEgraph.withState(EgraphState.PassedBiometrics).save()
    val failedBiometrics = TestData.newSavedOrder().newEgraph.withState(EgraphState.FailedBiometrics).save()
    val approvedByAdmin = TestData.newSavedOrder().newEgraph.withState(EgraphState.ApprovedByAdmin).save()
    val rejectedByAdmin = TestData.newSavedOrder().newEgraph.withState(EgraphState.RejectedByAdmin).save()
    val awaitingVerification = TestData.newSavedOrder().newEgraph.withState(EgraphState.AwaitingVerification).save()
    val published = TestData.newSavedOrder().newEgraph.withState(EgraphState.Published).save()

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

    val passedBiometrics = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.PassedBiometrics).save()
    val failedBiometrics = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.FailedBiometrics).save()
    val approvedByAdmin = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.ApprovedByAdmin).save()
    val rejectedByAdmin = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.RejectedByAdmin).save()
    val awaitingVerification = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.AwaitingVerification).save()
    val published = TestData.newSavedOrder(product).newEgraph.withState(EgraphState.Published).save()

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