package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import libs.Time
import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationAfter, CreatedUpdatedEntityTests, SavingEntityTests}
import java.util.Date
import java.sql.Timestamp

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
      profilePhotoUpdated = Some(new Timestamp(new Date().getTime))
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

}