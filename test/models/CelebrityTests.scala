package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import java.io.File
import javax.imageio.ImageIO
import libs.{ImageUtil, Time}
import ImageUtil.Conversions._

class CelebrityTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Celebrity]
  with CreatedUpdatedEntityTests[Celebrity]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{

  //
  // SavingEntityTests[Celebrity] methods
  //
  override def newEntity = {
    Celebrity()
  }

  override def saveEntity(toSave: Celebrity) = {
    Celebrity.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    Celebrity.findById(id)
  }

  override def transformEntity(toTransform: Celebrity) = {
    toTransform.copy(
      apiKey = Some("apiKey"),
      description = Some("desc"),
      popularName = Some("pname"),
      profilePhotoUpdated = Some(Time.now)
    )
  }

  //
  // Test cases
  //
  "A Celebrity" should "render to API format properly" in {
    val celeb = Celebrity(
      firstName = Some("Will"),
      lastName = Some("Chan"),
      popularName = Some("Wizzle")
    ).save()

    val apiMap = celeb.renderedForApi

    apiMap("firstName") should be ("Will")
    apiMap("lastName") should be ("Chan")
    apiMap("popularName") should be ("Wizzle")
    apiMap("id") should be (celeb.id)
    apiMap("created") should be (Time.toApiFormat(celeb.created))
    apiMap("updated") should be (Time.toApiFormat(celeb.updated))
  }

  it should "start with no profile photo" in {
    Celebrity().profilePhotoUpdated should be (None)
    Celebrity().profilePhoto should be (None)
  }

  it should "throw an exception if you save profile photo when id is 0" in {
    val celeb = makeCeleb
    val image = ImageIO.read(new File("test/files/image.png"))

    evaluating { celeb.saveWithProfilePhoto(image.asByteArray(ImageAsset.Png)) } should produce [IllegalArgumentException]
  }

  it should "store and retrieve the profile image asset" in {
    val celeb = makeCeleb
    val image = ImageIO.read(new File("test/files/image.png"))

    val (savedCeleb, imageAsset) = celeb.save().saveWithProfilePhoto(image.asByteArray(ImageAsset.Png))

    imageAsset.key should include ("celebrity/1")
    savedCeleb.profilePhotoUpdated.get.getTime should be (Time.now.getTime plusOrMinus 1000)
    savedCeleb.profilePhoto should not be (None)

    val profilePhoto = savedCeleb.profilePhoto.get
    profilePhoto.renderFromMaster.asByteArray(ImageAsset.Png).length should be (imageAsset.renderFromMaster.asByteArray(ImageAsset.Png).length)
  }

  def makeCeleb: Celebrity = {
    Celebrity(firstName=Some("Will"), lastName=Some("Chan"), popularName=Some("Wizzle"))
  }

}