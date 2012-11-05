package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import models.PrintOrder
import models.PrintOrderStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status

@RunWith(classOf[JUnitRunner])
class RequirePrintOrderIdTests extends EgraphsUnitTest {
  val goodId = 309812309808L
  val badId = 1329812398123L

  val printOrder = PrintOrder(services = null)

  "filter" should "allow printOrder ids that are associated with an printOrder" in {
    val errorOrPrintOrder = filterWithMocks.filter(goodId)

    errorOrPrintOrder should be(Right(printOrder))
  }

  it should "not allow printOrder ids that are not associated with an printOrder" in {
    val errorOrPrintOrder = filterWithMocks.filter(badId)
    val result = errorOrPrintOrder.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequirePrintOrderId = {
    new RequirePrintOrderId(mockPrintOrderStore)
  }

  private def mockPrintOrderStore = {
    import services.db.Schema
    class MockablePrintOrderStore(schema: Schema) extends PrintOrderStore(schema) {
      override def findById(id: Long): Option[PrintOrder] = { None }
    }

    val printOrderStore = mock[MockablePrintOrderStore]

    printOrderStore.findById(goodId) returns Some(printOrder)
    printOrderStore.findById(badId) returns None

    printOrderStore
  }
}