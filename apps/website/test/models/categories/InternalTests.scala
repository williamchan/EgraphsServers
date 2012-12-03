package models.categories
import org.junit.runner.RunWith
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import utils.DBTransactionPerTest
import services.AppConfig

@RunWith(classOf[JUnitRunner])
class InternalTests extends EgraphsUnitTest with DBTransactionPerTest {

  "ensureCategoryIsCreated" should "create a internal category if not there already" in new EgraphsTestApplication {
    deleteInternalCategory()

    val category = internal.ensureCategoryIsCreated()

    val featuredCategory = categoryStore.findByName(Internal.categoryName)
    val defined = 'defined
    featuredCategory should be(defined)
    featuredCategory.get.name should be(Internal.categoryName)
    featuredCategory.get.publicName should be(Internal.categoryName)
    Some(category) should be(featuredCategory)
  }

  it should "return a internal category if it is there already" in new EgraphsTestApplication {
    internal.ensureCategoryIsCreated() // this should make sure one already exists in the next call
    val category = internal.ensureCategoryIsCreated()

    category.name should be(Internal.categoryName)
    category.publicName should be(Internal.categoryName)
  }

  def deleteInternalCategory(): Unit = {
    val maybeCategory = categoryStore.findByName(Internal.categoryName)
    maybeCategory match {
      case Some(category) => categoryStore.delete(category)
      case None => // it's not there no need to delete
    }
  }

  def categoryStore = AppConfig.instance[CategoryStore]

  def internal = {
    AppConfig.instance[Internal]
  }
}