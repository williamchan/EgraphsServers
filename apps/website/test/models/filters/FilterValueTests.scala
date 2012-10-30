package models.filters

import utils._
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

class FilterValueTests  extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[FilterValue]
  with CreatedUpdatedEntityTests[Long, FilterValue]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def filterValueStore = AppConfig.instance[FilterValueStore]

  //
  // SavingEntityTests[FilterValue]
  //

  override def newEntity = {
    FilterValue(name = TestData.generateUsername(), publicName = TestData.generateUsername(), filterId = TestData.newSavedFilter.id)
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

    it should "require a name" in new EgraphsTestApplication {
      val exception = intercept[IllegalArgumentException] {FilterValue().save()}
      exception.getLocalizedMessage should include("FilterValue: name must be specified")
    }

    "FilterValue" should "require a publicName" in new EgraphsTestApplication {
      val exception = intercept[IllegalArgumentException] {FilterValue(name = "derp").save()}
      exception.getLocalizedMessage should include("FilterValue: publicName must be specified")
    }

    "FilterValue" should "not have duplicate names" in new EgraphsTestApplication {

      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val exception = intercept[RuntimeException] {
        FilterValue(name = filterValue.name, publicName = TestData.generateUsername(), filterId=TestData.newSavedFilter.id).save()
      }
      exception.getLocalizedMessage should include("ERROR: duplicate key value violates unique constraint ")
    }

    "FilterValue" should "allow duplicate publicNames" in new EgraphsTestApplication {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val filterValue2 = FilterValue(name=TestData.generateFullname(), publicName = filterValue.publicName, filterId = TestData.newSavedFilter.id).save()
      filterValue2.publicName should be (filterValue.publicName)
    }

    "FilterValue" should "return a child filter" in new EgraphsTestApplication {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val childFilter = TestData.newSavedFilter

      filterValue.filters.associate(childFilter)

      filterValue.filters.exists(f => f.id == childFilter.id) should be (true)

    }

    "FilterValue" should "return many child filters" in new EgraphsTestApplication {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val childFilters = for ( i <- 0 until 10) yield TestData.newSavedFilter
      childFilters.map(cf => filterValue.filters.associate(cf))

      filterValue.filters.size should be (childFilters.size)

      childFilters.map(cf =>
        filterValue.filters.exists(f => f.id == cf.id)
      )
    }

    "FilterValue" should "return associated celebrities" in new EgraphsTestApplication {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val celebrity = TestData.newSavedCelebrity()

      celebrity.filterValues.associate(filterValue)
      filterValue.celebrities.exists(c => c.id == celebrity.id ) should be (true)
    }
    "FilterStore" should "return by name" in new EgraphsTestApplication {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val retrieved = filterValueStore.findByName(filterValue.name).headOption.get
      retrieved.id should be (filterValue.id)
    }
}
