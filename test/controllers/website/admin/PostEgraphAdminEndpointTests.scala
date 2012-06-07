package controllers.website.admin

import org.junit.Assert._
import org.junit.Test
import scala.collection.JavaConversions._
import models.Egraph.EgraphState
import EgraphState._
import services.AppConfig
import services.db.{DBSession, TransactionSerializable}
import play.test.FunctionalTest._
import models.{EgraphStore, Egraph}
import utils.TestData

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
      egraph1 = Egraph(orderId = order.id).withState(PassedBiometrics).save()
      egraph2 = Egraph(orderId = order.id).withState(FailedBiometrics).save()
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
      val order = TestData.newSavedOrder()
      egraph1 = Egraph(orderId = order.id).withState(PassedBiometrics).save()
      egraph2 = Egraph(orderId = order.id).withState(FailedBiometrics).save()
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
    val egraph = db.connected(TransactionSerializable) {
      val order = TestData.newSavedOrder()
      Egraph(orderId = order.id).withState(ApprovedByAdmin).save()
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
