package models.categories

import org.junit.runner.RunWith
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import utils.DBTransactionPerTest
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class FeaturedTests extends EgraphsUnitTest with DBTransactionPerTest {

  "ensureCategoryValueIsCreated" should "create a featured category value and internal category are not already there" in new EgraphsTestApplication {
    deleteFeaturedCategoryValueRelationships()
    deleteFeaturedCategoryValue()
    deleteInternalCategory()

    val featured = featuredToTest
    val categoryValue = featured.categoryValue

    val featuredCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    val defined = 'defined
    featuredCategoryValue should be(defined)
    featuredCategoryValue.get.name should be(Featured.categoryValueName)
    featuredCategoryValue.get.publicName should be(Featured.categoryValueName)
    Some(categoryValue) should be(featuredCategoryValue)
  }

  it should "return a featured category value if it is there already" in new EgraphsTestApplication {
    val featured = featuredToTest
    featured.categoryValue // this should make sure one already exists in the next call
    val categoryValue = featured.categoryValue

    categoryValue.name should be(Featured.categoryValueName)
    categoryValue.publicName should be(Featured.categoryValueName)
  }

  def deleteInternalCategory(): Unit = {
    val maybeCategory = categoryStore.findByName(Internal.categoryName)
    maybeCategory match {
      case Some(category) => categoryStore.delete(category)
      case None => // it's not there no need to delete
    }
  }

  def deleteFeaturedCategoryValue(): Unit = {
    val maybeCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeCategoryValue match {
      case Some(categoryValue) => categoryValueStore.delete(categoryValue)
      case None => // it's not there no need to delete
    }
  }

  def deleteFeaturedCategoryValueRelationships(): Unit = {
    val maybeCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeCategoryValue match {
      case Some(categoryValue) =>
        val relationships = categoryValueRelationshipStore.findByCategoryValueId(categoryValue.id).toList
        for(relationship <- relationships) {
          categoryValueRelationshipStore.delete(relationship)
        }
        
      case None =>
    }
  }

  def categoryStore = AppConfig.instance[CategoryStore]
  def categoryValueStore = AppConfig.instance[CategoryValueStore]
  def categoryValueRelationshipStore = AppConfig.instance[CategoryValueRelationshipStore]

  def featuredToTest = {
    AppConfig.instance[Featured]
  }

  def internal = {
    AppConfig.instance[Internal]
  }
}