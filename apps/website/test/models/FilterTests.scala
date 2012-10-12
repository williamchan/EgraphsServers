package models

import filters.{FilterValue, FilterStore, Filter}
import utils._
import services.http.DBTransaction
import services.AppConfig


class FilterTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[Filter]
  with CreatedUpdatedEntityTests[Long, Filter]
  with DBTransactionPerTest
{
  val filterStore = AppConfig.instance[FilterStore]

  //
  // SavingEntityTests[Filter]
  //

  override def newEntity = {
    Filter(name = TestData.generateUsername(), publicname = TestData.generateUsername())
  }

  override def saveEntity(toSave: Filter) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    filterStore.findById(id)
  }

  override def transformEntity(toTransform: Filter) = {
    toTransform.copy(
     name = TestData.generateUsername()
    )
  }

  //
  // Test cases
  //

  "Filter" should "require a name" in {
    val exception = intercept[IllegalArgumentException] {Filter(publicname = "herp").save()}
    exception.getLocalizedMessage should include("Filter: name must be specified")
  }

  "Filter" should "require a publicname" in {
    val exception = intercept[IllegalArgumentException] {Filter(name = "herp").save()}
    exception.getLocalizedMessage should include("Filter: publicname must be specified")
  }

//  "Filter" should "return all associated FilterValues" in {
//
//    val filter = newEntity
//    val filterValue = generateFilterValue
//
//    val filterWithAssociatedValue = filter.include(filterValue).save()
//
//    assert(filterWithAssociatedValue.filterValues.contrains(filterValue))
//  }

  private def generateFilterValue : FilterValue =
    new FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername())

}
