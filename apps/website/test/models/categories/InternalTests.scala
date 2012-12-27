package models.categories

import org.junit.runner.RunWith
import utils.EgraphsUnitTest
import org.scalatest.junit.JUnitRunner
import utils.DBTransactionPerTest
import services.AppConfig
import services.db.DBSession
import services.db.TransactionSerializable

@RunWith(classOf[JUnitRunner])
class InternalTests extends EgraphsUnitTest {
  private def db = AppConfig.instance[DBSession]

  "ensureCategoryIsCreated" should "create a internal category if not there already" in new EgraphsTestApplication {
    db.connected(TransactionSerializable) {
      tryToDeleteInternalCategory()
    }

    db.connected(TransactionSerializable) {
      val category = internal.category

      val featuredCategory = categoryStore.findByName(Internal.categoryName)
      val defined = 'defined
      featuredCategory should be(defined)
      featuredCategory.get.name should be(Internal.categoryName)
      featuredCategory.get.publicName should be(Internal.categoryName)
      Some(category) should be(featuredCategory)
    }
  }

  it should "return a internal category if it is there already" in new EgraphsTestApplication {
    val internalInstance = internal
    db.connected(TransactionSerializable) {
      internalInstance.category // this should make sure one already exists in the next call
    }

    val category = internalInstance.category

    category.name should be(Internal.categoryName)
    category.publicName should be(Internal.categoryName)

  }

  def tryToDeleteInternalCategory(): Unit = {
    val maybeCategory = categoryStore.findByName(Internal.categoryName)
    maybeCategory match {
      case Some(category) =>
        try {
          categoryStore.delete(category)
        } catch {
          case _ => //do nothing, we accept that we may not be able to remove Internal
          // and that other tests may break if running at the same time if we do this.
        }
      case None => // it's not there no need to delete
    }
  }

  def categoryStore = AppConfig.instance[CategoryStore]

  def internal = {
    AppConfig.instance[Internal]
  }
}