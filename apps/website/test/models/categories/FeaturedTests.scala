package models.categories

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.CelebrityStore
import services.AppConfig
import utils.DBTransactionPerTest
import utils.EgraphsUnitTest
import utils.TestData
import models.enums.PublishedStatus

@RunWith(classOf[JUnitRunner])
class FeaturedTests extends EgraphsUnitTest with DBTransactionPerTest {

  "categoryValue" should "create a featured category value and internal category are not already there" in new EgraphsTestApplication {
    deleteFeaturedCategoryValue()

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

  "updateFeaturedCelebrities" should "remove celebs that weren't in the updated featured list" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured = true, includeCelebInNewFeaturedCelebs = false) should be(false)
  }

  it should "keep featured celebs" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured = true, includeCelebInNewFeaturedCelebs = true) should be(true)
  }

  it should "set newly featured celebs" in new EgraphsTestApplication {
    featuredStateOfCelebWhen(celebWasFeatured = false, includeCelebInNewFeaturedCelebs = true) should be(true)
  }

  private def featuredStateOfCelebWhen(
    celebWasFeatured: Boolean,
    includeCelebInNewFeaturedCelebs: Boolean): Boolean = {

    val featuredCelebrity = TestData.newSavedCelebrity()

    if (celebWasFeatured) {
      featuredToTest.updateFeaturedCelebrities(List(featuredCelebrity.id))
    }

    val newFeaturedCelebs = if (includeCelebInNewFeaturedCelebs) {
      List(featuredCelebrity.id)
    } else {
      List(TestData.newSavedCelebrity().id) // a list of celebrity ids that can't be this celebrity
    }

    featuredToTest.updateFeaturedCelebrities(newFeaturedCelebs)

    featuredToTest.categoryValue.celebrities exists (celebrity => celebrity.id == featuredCelebrity.id)
  }

  private def deleteFeaturedCategoryValue(): Unit = {
    val maybeCategoryValue = categoryValueStore.findByName(Featured.categoryValueName)
    maybeCategoryValue match {
      case Some(categoryValue) => categoryValueStore.delete(categoryValue)
      case None => // it's not there no need to delete
    }
  }

  private def celebrityStore = AppConfig.instance[CelebrityStore]
  private def categoryValueStore = AppConfig.instance[CategoryValueStore]

  private def featuredToTest = AppConfig.instance[Featured]
}