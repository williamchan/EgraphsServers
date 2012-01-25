package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}
import services.AppConfig

class AdministratorTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Administrator]
  with CreatedUpdatedEntityTests[Administrator]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{
  val adminStore = AppConfig.instance[AdministratorStore]

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Administrator()
  }

  override def saveEntity(toSave: Administrator) = {
    adminStore.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    adminStore.findById(id)
  }

  override def transformEntity(toTransform: Administrator) = {
    // No Administrator-specific data here to transform yet!
    toTransform.copy(role=Some("big boss man"))
  }


  //
  // Test cases
  //

}