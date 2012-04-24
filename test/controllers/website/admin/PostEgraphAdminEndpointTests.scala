package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import models.Egraph.EgraphState
import EgraphState._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import play.test.FunctionalTest._
import utils.FunctionalTestUtils._
import models.{EgraphStore, EgraphTests, Egraph}

class PostEgraphAdminEndpointTests extends AdminFunctionalTest with CleanDatabaseAfterEachTest {

  private val db = AppConfig.instance[DBSession]
  private val egraphStore = AppConfig.instance[EgraphStore]

  @Test
  def testApproveEgraph() {
    createAndLoginAsAdmin()
    var egraph1: Egraph = null
    var egraph2: Egraph = null
    db.connected(TransactionSerializable) {
      val order = EgraphTests.persistedOrder()
      egraph1 = Egraph(orderId = order.id).withState(PassedBiometrics).saveWithoutAssets()
      egraph2 = Egraph(orderId = order.id).withState(FailedBiometrics).saveWithoutAssets()
    }

    val response1 = POST("/admin/egraphs/" + egraph1.id, getPostStrParams("ApprovedByAdmin"))
    assertStatus(302, response1)
    val response2 = POST("/admin/egraphs/" + egraph2.id, getPostStrParams("ApprovedByAdmin"))
    assertStatus(302, response2)

    db.connected(TransactionSerializable) {
      assertEquals(ApprovedByAdmin, egraphStore.findById(egraph1.id).get.state)
      assertEquals(ApprovedByAdmin, egraphStore.findById(egraph2.id).get.state)
    }
  }

  @Test
  def testRejectEgraph() {
    createAndLoginAsAdmin()
    var egraph1: Egraph = null
    var egraph2: Egraph = null
    db.connected(TransactionSerializable) {
      val order = EgraphTests.persistedOrder()
      egraph1 = Egraph(orderId = order.id).withState(PassedBiometrics).saveWithoutAssets()
      egraph2 = Egraph(orderId = order.id).withState(FailedBiometrics).saveWithoutAssets()
    }

    val response1 = POST("/admin/egraphs/" + egraph1.id, getPostStrParams("RejectedByAdmin"))
    assertStatus(302, response1)
    val response2 = POST("/admin/egraphs/" + egraph2.id, getPostStrParams("RejectedByAdmin"))
    assertStatus(302, response2)

    db.connected(TransactionSerializable) {
      assertEquals(RejectedByAdmin, egraphStore.findById(egraph1.id).get.state)
      assertEquals(RejectedByAdmin, egraphStore.findById(egraph2.id).get.state)
    }
  }

  @Test
  def testPublishEgraph() {
    createAndLoginAsAdmin()
    var egraph = db.connected(TransactionSerializable) {
      val order = EgraphTests.persistedOrder()
      Egraph(orderId = order.id).withState(ApprovedByAdmin).saveWithoutAssets()
    }

    val response = POST("/admin/egraphs/" + egraph.id, getPostStrParams("Published"))
    assertStatus(302, response)

    db.connected(TransactionSerializable) {
      assertEquals(Published, egraphStore.findById(egraph.id).get.state)
    }
  }

  @Test
  def testPublishingEgraphAlsoSendsEmail() {
    // TODO(wchan): How to test this?
  }

  private def getPostStrParams(egraphState: String): Map[String, String] = {
    Map[String, String]("egraphState" -> egraphState)
  }

}
