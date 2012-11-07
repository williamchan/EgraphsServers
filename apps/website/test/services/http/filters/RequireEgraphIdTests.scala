package services.http.filters

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import models.Egraph
import models.EgraphStore
import services.http.filters.FilterTestUtil.EitherErrorOrSuccess2RichErrorOrSuccess
import utils.EgraphsUnitTest
import play.api.test.Helpers.NOT_FOUND
import play.api.test.Helpers.status

@RunWith(classOf[JUnitRunner])
class RequireEgraphIdTests extends EgraphsUnitTest {
  val goodId = 309812309808L
  val badId = 1329812398123L

  val egraph = Egraph(services = null)

  "filter" should "allow egraph ids that are associated with an egraph" in {
    val errorOrEgraph = filterWithMocks.filter(goodId)

    errorOrEgraph should be(Right(egraph))
  }

  it should "not allow egraph ids that are not associated with an egraph" in {
    val errorOrEgraph = filterWithMocks.filter(badId)
    val result = errorOrEgraph.toErrorOrOkResult

    status(result) should be(NOT_FOUND)
  }

  private def filterWithMocks: RequireEgraphId = {
    new RequireEgraphId(mockEgraphStore)
  }

  private def mockEgraphStore = {
    import services.db.Schema
    class MockableEgraphStore(schema: Schema) extends EgraphStore(schema) {
      override def findById(id: Long): Option[Egraph] = { None }
    }

    val egraphStore = mock[MockableEgraphStore]

    egraphStore.findById(goodId) returns Some(egraph)
    egraphStore.findById(badId) returns None

    egraphStore
  }
}