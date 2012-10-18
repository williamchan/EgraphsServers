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
    FilterValue(name = TestData.generateUsername(), publicname = TestData.generateUsername(), filterId = TestData.newSavedFilter.id)
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

    "FilterValue" should "require a publicname" in {
      val exception = intercept[IllegalArgumentException] {FilterValue(name = "derp").save()}
      exception.getLocalizedMessage should include("FilterValue: publicname must be specified")
    }

    "FilterValue" should "not have duplicate names" in {

      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val exception = intercept[RuntimeException] {
        FilterValue(name = filterValue.name, publicname = TestData.generateUsername(), filterId=TestData.newSavedFilter.id).save()
      }
      exception.getLocalizedMessage should include("ERROR: duplicate key value violates unique constraint ")
    }

    "FilterValue" should "allow duplicate publicnames" in {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val filterValue2 = FilterValue(name=TestData.generateFullname(), publicname = filterValue.publicname, filterId = TestData.newSavedFilter.id).save()
      filterValue2.publicname should be (filterValue.publicname)
    }

    "FilterValue" should "return a child filter" in {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val childFilter = TestData.newSavedFilter

      filterValue.filters.associate(childFilter)

      filterValue.filters.exists(f => f.id == childFilter.id) should be (true)

    }

    "FilterValue" should "return many child filters" in {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val childFilters = for ( i <- 0 until 10) yield TestData.newSavedFilter
      childFilters.map(cf => filterValue.filters.associate(cf))

      filterValue.filters.size should be (childFilters.size)

      childFilters.map(cf =>
        filterValue.filters.exists(f => f.id == cf.id)
      )
    }

    "FilterValue" should "return associated celebrities" in {
      val filterValue = TestData.newSavedFilterValue(TestData.newSavedFilter.id)
      val celebrity = TestData.newSavedCelebrity()

      celebrity.filterValues.associate(filterValue)
      filterValue.celebrities.exists(c => c.id == celebrity.id ) should be (true)
    }
}
