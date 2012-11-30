package models.categories
import org.junit.runner.RunWith
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import utils.DBTransactionPerTest
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class FeaturedTests extends EgraphsUnitTest with DBTransactionPerTest {

  "ensureCategoryIsCreated" should "create a featured category if not there already" in new EgraphsTestApplication {
    deleteFeaturedCategory()

    val featured = featuredToTest
    val category = featured.ensureCategoryIsCreated()

    val featuredCategory = categoryStore.findByName(Featured.categoryName)
    val defined = 'defined
    featuredCategory should be(defined)
    featuredCategory.get.name should be(Featured.categoryName)
    featuredCategory.get.publicName should be(Featured.categoryName)
    Some(category) should be(featuredCategory)
  }

  it should "return a featured category if it is there already" in new EgraphsTestApplication {
    val featured = featuredToTest
    featured.ensureCategoryIsCreated() // this should make sure one already exists in the next call
    val category = featured.ensureCategoryIsCreated()

    category.name should be(Featured.categoryName)
    category.publicName should be(Featured.categoryName)
  }

  "ensureCategoryValueIsCreated" should "create a featured category value and category are not already there" in new EgraphsTestApplication {
    deleteFeaturedCategoryValue()
    deleteFeaturedCategory()

    val featured = featuredToTest
    val categoryValue = featured.ensureCategoryValueIsCreated()

    val featuredCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    val defined = 'defined
    featuredCategoryValue should be(defined)
    featuredCategoryValue.get.name should be(Featured.categoryValueName)
    featuredCategoryValue.get.publicName should be(Featured.categoryValueName)
    Some(categoryValue) should be(featuredCategoryValue)
  }

  it should "return a featured category value if it is there already" in new EgraphsTestApplication {
    val featured = featuredToTest
    featured.ensureCategoryValueIsCreated() // this should make sure one already exists in the next call
    val categoryValue = featured.ensureCategoryValueIsCreated()

    categoryValue.name should be(Featured.categoryValueName)
    categoryValue.publicName should be(Featured.categoryValueName)
  }

  def deleteFeaturedCategory(): Unit = {
    val maybeCategory = categoryStore.findByName(Featured.categoryName)
    maybeCategory match {
      case Some(category) => categoryStore.delete(category)
      case None => // it's not there no need to delete
    }
  }

  def deleteFeaturedCategoryValue(): Unit = {
    val maybeCategory = categoryValueStore.findByName(Featured.categoryValueName)
    maybeCategory match {
      case Some(categoryValue) => categoryValueStore.delete(categoryValue)
      case None => // it's not there no need to delete
    }
  }

  def categoryStore = AppConfig.instance[CategoryStore]
  def categoryValueStore = AppConfig.instance[CategoryValueStore]

  def featuredToTest = {
    AppConfig.instance[Featured]
  }
}