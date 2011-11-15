package models

import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.ShouldMatchers
import play.test.UnitFlatSpec
import utils.{DBTransactionPerTest, SavingEntityTests, CreatedUpdatedEntityTests, ClearsDatabaseAndValidationAfter}

class AdministratorTests extends UnitFlatSpec
  with ShouldMatchers
  with BeforeAndAfterEach
  with SavingEntityTests[Administrator]
  with CreatedUpdatedEntityTests[Administrator]
  with ClearsDatabaseAndValidationAfter
  with DBTransactionPerTest
{

  //
  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Administrator()
  }

  override def saveEntity(toSave: Administrator) = {
    Administrator.save(toSave)
  }

  override def restoreEntity(id: Long) = {
    Administrator.findById(id)
  }

  override def transformEntity(toTransform: Administrator) = {
    // No Administrator-specific data here to transform yet!
    toTransform.copy(role=Some("big boss man"))
  }


  //
  // Test cases
  //

}