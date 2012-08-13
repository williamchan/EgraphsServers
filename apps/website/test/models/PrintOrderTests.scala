package models

import enums.EgraphState
import utils._
import services.AppConfig

class PrintOrderTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
  with SavingEntityTests[PrintOrder]
  with CreatedUpdatedEntityTests[PrintOrder]
  with DBTransactionPerTest {

  private val store = AppConfig.instance[PrintOrderStore]

  override def newEntity = {
    val order = TestData.newSavedOrder()
    PrintOrder(orderId = order.id)
  }

  override def saveEntity(toSave: PrintOrder) = {
    toSave.save()
  }

  override def restoreEntity(id: Long) = {
    store.findById(id)
  }

  override def transformEntity(toTransform: PrintOrder) = {
    toTransform.copy(pngUrl = Some("myUrl"))
  }

  "generatePng" should "return URL that points to PNG" in {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val printOrder = PrintOrder(orderId = order.id).save()
    printOrder.generatePng(100) should be(None)

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val pngUrl: Option[String] = printOrder.generatePng(100)
    pngUrl.get should endWith("blob/files/egraphs/" + egraph.id + "/image/signing-origin-offset-0x0_global-width-100px-v1.png")
  }
}
