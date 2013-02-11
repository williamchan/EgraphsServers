package models

import enums.HasPublishedStatusTests
import utils._
import services.AppConfig

class MastheadTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Masthead]
  with CreatedUpdatedEntityTests[Long, Masthead]
  with DateShouldMatchers
  with DBTransactionPerTest
  with HasPublishedStatusTests[Masthead]
{
  def store = AppConfig.instance[MastheadStore]


  //
  //  HasPublishedStatus[Celebrity] methods
  //
  override def newPublishableEntity = {
    Masthead(name = TestData.generateFullname(), headline = TestData.generateUsername())
  }


  // SavingEntityTests[Account] methods
  //
  override def newEntity = {
    Masthead(name = TestData.generateUsername(), headline = TestData.generateUsername())
  }

  override def saveEntity(toSave: Masthead) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: Masthead) = {
    toTransform.copy(
      name = "name"
    )
  }

}
