package models.filters

import utils._
import services.AppConfig
import models.Celebrity


class CelebrityFilterValueTests extends EgraphsUnitTest
  with SavingEntityIdLongTests[CelebrityFilterValue]
  with CreatedUpdatedEntityTests[Long, CelebrityFilterValue]
  with DBTransactionPerTest
{
  val celebrityFilterValueStore = AppConfig.instance[CelebrityFilterValueStore]

  //
  // SavingEntityTests[CelebrityFilterValue]
  //

  override def newEntity = {
    val filterValue = new FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername()).save()
    val celebrity = TestData.newSavedCelebrity()
    CelebrityFilterValue(celebrityId = celebrity.id, filterValueId = filterValue.id)
  }

  override def saveEntity(toSave: CelebrityFilterValue) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    celebrityFilterValueStore.findById(id)
  }

  override def transformEntity(toTransform: CelebrityFilterValue) = {
    val filterValue = new FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername()).save()

    toTransform.copy(
      filterValueId = filterValue.id
    )
  }

  //
  // Test cases
  //

  "CelebrityFilterValue" should "require a celebrity id" in {
    val exception = intercept[IllegalArgumentException] {CelebrityFilterValue().save()}
    exception.getLocalizedMessage should include("CelebrityFilterValue: celebrity id must be specified")
  }

  "CelebrityFilterValue" should "require a filter value id" in {
    val exception = intercept[IllegalArgumentException] {CelebrityFilterValue(celebrityId = 5).save()}
    exception.getLocalizedMessage should include("CelebrityFilterValue: filter value id must be specified")
  }

  "CelebrityFilterValue" should "return all filterValues associated with a celebrity" in {

  }
}

object FilterData {

//  def generateTaggedCelebrity : (Celebrity, Filter, FilterValue, FilterValueRelationship, CelebrityFilterValue) = {
//    val celeb = TestData.newSavedCelebrity()
//    val filter = Filter
//
//  }


}
