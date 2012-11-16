package models.categories

import utils._
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class CategoryTests extends EgraphsUnitTest
 with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[Category]
  with CreatedUpdatedEntityTests[Long, Category]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def categoryStore = AppConfig.instance[CategoryStore]

  //
  // SavingEntityTests[Category]
  //

  override def newEntity = {
    Category(name = TestData.generateUsername(), publicName = TestData.generateUsername())
  }

  override def saveEntity(toSave: Category) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    categoryStore.findById(id)
  }

  override def transformEntity(toTransform: Category) = {
    toTransform.copy(
     name = TestData.generateUsername()
    )
  }

  //
  // Test cases
  //

  "Category" should "require a name" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Category(publicName = "herp").save()}
    exception.getLocalizedMessage should include("Category: name must be specified")
  }

  "Category" should "require a publicName" in new EgraphsTestApplication {
    val exception = intercept[IllegalArgumentException] {Category(name = "herp").save()}
    exception.getLocalizedMessage should include("Category: publicName must be specified")
  }

  "Category" should "return an associated value" in new EgraphsTestApplication {
    val category = TestData.newSavedCategory
    val categoryValue = TestData.newSavedCategoryValue(category.id)
    val categoryValues = category.categoryValues
    categoryValues.exists(fv => fv.id == categoryValue.id) should be (true)
  }

  "Category" should "return all associated values" in new EgraphsTestApplication {
    val category = TestData.newSavedCategory
    val newCategoryValues = for ( i <- 0 until 10) yield TestData.newSavedCategoryValue(category.id)
    val retrievedCategoryValues = category.categoryValues

    retrievedCategoryValues.size should be (newCategoryValues.size)

    newCategoryValues.map(fv =>
      retrievedCategoryValues.exists(rfv => rfv.id == fv.id) should be (true)
    )
  }
  
  "CategoryStore" should "return by name" in new EgraphsTestApplication {
    val category = TestData.newSavedCategory
    val retrieved = categoryStore.findByName(category.name).headOption.get
    retrieved.id should be (category.id)
  }
}
