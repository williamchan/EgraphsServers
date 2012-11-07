package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import models.InventoryBatch
import models.InventoryBatchStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status
import utils.EgraphsUnitTest
import services.AppConfig
import utils.TestData
import utils.DBTransactionPerTest

@RunWith(classOf[JUnitRunner])
class RequireOrderIdOfCelebrityTests extends EgraphsUnitTest with DBTransactionPerTest {

  val badId = Long.MaxValue

  "filter" should "allow order ids that are associated with an order and a celebrity" in new EgraphsTestApplication {
    val order = TestData.newSavedOrder()
    val errorOrOrder = filter.filter(order.id, order.product.celebrityId)

    errorOrOrder should be(Right(order))
  }

  it should "not allow order ids that are associated with an order but not a celebrity" in new EgraphsTestApplication {
    val order = TestData.newSavedOrder()
    val errorOrOrder = filter.filter(order.id, badId)
    val result = errorOrOrder.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  it should "not allow order ids that are associated with a celebrity but not an order" in new EgraphsTestApplication {
    val celebrity = TestData.newSavedCelebrity()
    val errorOrOrder = filter.filter(badId, celebrity.id)
    val result = errorOrOrder.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filter: RequireOrderIdOfCelebrity = {
    AppConfig.instance[RequireOrderIdOfCelebrity]
  }
}
