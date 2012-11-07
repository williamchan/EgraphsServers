package models.categories

import utils._
import services.AppConfig
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

class CategoryValueTests  extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[CategoryValue]
  with CreatedUpdatedEntityTests[Long, CategoryValue]
  with DateShouldMatchers
  with DBTransactionPerTest
{
  def categoryValueStore = AppConfig.instance[CategoryValueStore]

  //
  // SavingEntityTests[CategoryValue]
  //

  override def newEntity = {
    CategoryValue(name = TestData.generateUsername(), publicName = TestData.generateUsername(), categoryId = TestData.newSavedCategory.id)
  }

  override def saveEntity(toSave: CategoryValue) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    categoryValueStore.findById(id)
  }

  override def transformEntity(toTransform: CategoryValue) = {
    toTransform.copy(
      name = TestData.generateUsername()
    )
  }

    //
    // Test cases
    //

    "CategoryValue" should "require a name" in new EgraphsTestApplication {
      val exception = intercept[IllegalArgumentException] {CategoryValue().save()}
      exception.getLocalizedMessage should include("CategoryValue: name must be specified")
    }

    it should "require a publicName" in new EgraphsTestApplication {
      val exception = intercept[IllegalArgumentException] {CategoryValue(name = "derp").save()}
      exception.getLocalizedMessage should include("CategoryValue: publicName must be specified")
    }

    it should "not have duplicate names" in new EgraphsTestApplication {

      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val exception = intercept[RuntimeException] {
        CategoryValue(name = categoryValue.name, publicName = TestData.generateUsername(), categoryId=TestData.newSavedCategory.id).save()
      }
      exception.getLocalizedMessage should include("ERROR: duplicate key value violates unique constraint ")
    }

    it should "allow duplicate publicNames" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val categoryValue2 = CategoryValue(name=TestData.generateFullname(), publicName = categoryValue.publicName, categoryId = TestData.newSavedCategory.id).save()
      categoryValue2.publicName should be (categoryValue.publicName)
    }

    it should "return a child category" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val childCategory = TestData.newSavedCategory

      categoryValue.categories.associate(childCategory)
      categoryValue.categories.exists(c => c.id == childCategory.id) should be (true)

    }

    it should "return many child categories" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val childCategories = for ( i <- 0 until 10) yield TestData.newSavedCategory
      childCategories.map(cf => categoryValue.categories.associate(cf))

      categoryValue.categories.size should be (childCategories.size)

      childCategories.map(cf =>
        categoryValue.categories.exists(c => c.id == cf.id)
      )
    }

    it should "return associated celebrities" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val celebrity = TestData.newSavedCelebrity()

      celebrity.categoryValues.associate(categoryValue)
      categoryValue.celebrities.exists(c => c.id == celebrity.id ) should be (true)
    }
    
    "CategoryStore" should "update a list of categoryIds" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val testCategory1 = TestData.newSavedCategory
      val testCategory2 = TestData.newSavedCategory
      val testCategory3 = TestData.newSavedCategory
      categoryValueStore.updateCategories(categoryValue, List(testCategory1.id, testCategory2.id))
       
      categoryValue.categories.size should be (2)
      
      categoryValueStore.updateCategories(categoryValue, List(testCategory3.id))
      
      categoryValue.categories.size should be (1)
    }
    
    it should "return by name" in new EgraphsTestApplication {
      val categoryValue = TestData.newSavedCategoryValue(TestData.newSavedCategory.id)
      val retrieved = categoryValueStore.findByName(categoryValue.name).headOption.get
      retrieved.id should be (categoryValue.id)
    }
    
    it should "return a celebrities categoryValue/Category pairs" in new EgraphsTestApplication {
      val celeb = TestData.newSavedCelebrity()
      val category1 = TestData.newSavedCategory
      val category2 = TestData.newSavedCategory
      val categoryValue1 = TestData.newSavedCategoryValue(category1.id)
      val categoryValue2 = TestData.newSavedCategoryValue(category2.id)
  
      celeb.categoryValues.associate(categoryValue1)
      celeb.categoryValues.associate(categoryValue2)
  
      val results = categoryValueStore.categoryValueCategoryPairs(celeb)
      results.size should be (2)
      results should contain((categoryValue1, category1))
      results should contain((categoryValue2, category2))
    }
    
    
}
