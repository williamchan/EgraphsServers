import models.{Account, Celebrity}
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.data.validation.Validation
import play.test.UnitFlatSpec

class CelebrityTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Celebrity]
  with CreatedUpdatedEntityTests[Celebrity]
  with ClearsDatabaseAndValidationAfter
{

  //
  // SavingEntityTests[Account] methods
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
      profilePhotoId = Some("photoKey")
    )
  }


  //
  // Test cases
  //

}