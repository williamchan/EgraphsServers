package models.filters

import utils._
import services.AppConfig

class FilterValueTests  extends EgraphsUnitTest
  with SavingEntityIdLongTests[FilterValue]
  with CreatedUpdatedEntityTests[Long, FilterValue]
  with DBTransactionPerTest
{
  val filterValueStore = AppConfig.instance[FilterValueStore]

  //
  // SavingEntityTests[FilterValue]
  //

  override def newEntity = {
    FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername())
  }

  override def saveEntity(toSave: FilterValue) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    filterValueStore.findById(id)
  }

  override def transformEntity(toTransform: FilterValue) = {
    toTransform.copy(
      name = TestData.generateUsername()
    )
  }
    //
    // Test cases
    //

    "FilterValue" should "require a name" in {
      val exception = intercept[IllegalArgumentException] {FilterValue().save()}
      exception.getLocalizedMessage should include("FilterValue: name must be specified")
    }
}
