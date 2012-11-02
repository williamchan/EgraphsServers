package models.filters

import utils._
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class FilterTests extends EgraphsUnitTest
 with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Filter]
  with CreatedUpdatedEntityTests[Long, Filter]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def filterStore = AppConfig.instance[FilterStore]

  //
  // SavingEntityTests[Filter]
  //

  override def newEntity = {
    Filter(name = TestData.generateUsername(), publicName = TestData.generateUsername())
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

  "Filter" should "require a name" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Filter(publicName = "herp").save()}
    exception.getLocalizedMessage should include("Filter: name must be specified")
  }

  "Filter" should "require a publicName" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Filter(name = "herp").save()}
    exception.getLocalizedMessage should include("Filter: publicName must be specified")
  }

  "Filter" should "return an associated value" in new EgraphsTestApplication {
    val filter = TestData.newSavedFilter
    val filterValue = TestData.newSavedFilterValue(filter.id)
    val filterValues = filter.filterValues
    filterValues.exists(fv => fv.id == filterValue.id) should be (true)
  }

  "Filter" should "return all associated values" in new EgraphsTestApplication {
    val filter = TestData.newSavedFilter
    val newFilterValues = for ( i <- 0 until 10) yield TestData.newSavedFilterValue(filter.id)
    val retrievedFilterValues = filter.filterValues

    retrievedFilterValues.size should be (newFilterValues.size)

    newFilterValues.map(fv =>
      retrievedFilterValues.exists(rfv => rfv.id == fv.id) should be (true)
    )
  }
  
  "FilterStore" should "return by name" in new EgraphsTestApplication {
    val filter = TestData.newSavedFilter
    val retrieved = filterStore.findByName(filter.name).headOption.get
    retrieved.id should be (filter.id)
  }
}
