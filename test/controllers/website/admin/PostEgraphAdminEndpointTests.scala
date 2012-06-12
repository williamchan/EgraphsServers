package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import play.test.FunctionalTest._
import models.{EgraphStore, Egraph}
import utils.TestData
import models.enums.EgraphState

class PostEgraphAdminEndpointTests extends AdminFunctionalTest {

  private val db = AppConfig.instance[DBSession]
  private val egraphStore = AppConfig.instance[EgraphStore]

  @Test
  def testApproveEgraph() {
    createAndLoginAsAdmin()
    var egraph1: Egraph = null
    var egraph2: Egraph = null
    db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder()
      egraph1 = Egraph(orderId = order.id).withEgraphState(EgraphState.PassedBiometrics).save()
      egraph2 = Egraph(orderId = order.id).withEgraphState(EgraphState.FailedBiometrics).save()
    }

    val response1 = POST("/admin/egraphs/" + egraph1.id, getPostStrParams("ApprovedByAdmin"))
    assertStatus(302, response1)
    val response2 = POST("/admin/egraphs/" + egraph2.id, getPostStrParams("ApprovedByAdmin"))
    assertStatus(302, response2)

    db.connected(TransactionSerializable) {
      assertEquals(EgraphState.ApprovedByAdmin, egraphStore.findById(egraph1.id).get.egraphState)
      assertEquals(EgraphState.ApprovedByAdmin, egraphStore.findById(egraph2.id).get.egraphState)
    }
  }

  @Test
  def testRejectEgraph() {
    createAndLoginAsAdmin()
    var egraph1: Egraph = null
    var egraph2: Egraph = null
    db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder()
      egraph1 = Egraph(orderId = order.id).withEgraphState(EgraphState.PassedBiometrics).save()
      egraph2 = Egraph(orderId = order.id).withEgraphState(EgraphState.FailedBiometrics).save()
    }

    val response1 = POST("/admin/egraphs/" + egraph1.id, getPostStrParams("RejectedByAdmin"))
    assertStatus(302, response1)
    val response2 = POST("/admin/egraphs/" + egraph2.id, getPostStrParams("RejectedByAdmin"))
    assertStatus(302, response2)

    db.connected(TransactionSerializable) {
      assertEquals(EgraphState.RejectedByAdmin, egraphStore.findById(egraph1.id).get.egraphState)
      assertEquals(EgraphState.RejectedByAdmin, egraphStore.findById(egraph2.id).get.egraphState)
    }
  }

  @Test
  def testPublishEgraph() {
    createAndLoginAsAdmin()
    val egraph = db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder()
      Egraph(orderId = order.id).withEgraphState(EgraphState.ApprovedByAdmin).save()
    }

    val response = POST("/admin/egraphs/" + egraph.id, getPostStrParams("Published"))
    assertStatus(302, response)

    db.connected(TransactionSerializable) {
      assertEquals(EgraphState.Published, egraphStore.findById(egraph.id).get.egraphState)
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
