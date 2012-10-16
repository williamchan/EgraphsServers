package models.filters

import utils._
import services.AppConfig


class FilterValueRelationshipTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[FilterValueRelationship]
  with CreatedUpdatedEntityTests[Long, FilterValueRelationship]
  with DBTransactionPerTest
{
  val filterValueRelationshipStore = AppConfig.instance[FilterValueRelationshipStore]

  //
  // SavingEntityTests[FilterValueRelationship]
  //

  override def newEntity = {
    val filter =  Filter(name = TestData.generateUsername(), publicname = TestData.generateUsername()).save()
    val filterValue = FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername()).save()
    FilterValueRelationship(filterId = filter.id, filterValueId = filterValue.id)
  }

  override def saveEntity(toSave: FilterValueRelationship) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    filterValueRelationshipStore.findById(id)
  }

  override def transformEntity(toTransform: FilterValueRelationship) = {
    val filterValue = FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername()).save()
    toTransform.copy(
      filterValueId = filterValue.id
    )
  }
  //
  // Test cases
  //

  "FilterValueRelationship" should "require a filter id" in {
    val exception = intercept[IllegalArgumentException] {FilterValueRelationship().save()}
    exception.getLocalizedMessage should include("FilterValueRelationship: filter id must be specified")
  }
}
