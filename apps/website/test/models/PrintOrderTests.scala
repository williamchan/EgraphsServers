package models

import enums.EgraphState
import utils._
import services.AppConfig
import services.blobs.Blobs
import Blobs.Conversions._

class PrintOrderTests extends EgraphsUnitTest
  with ClearsCacheAndBlobsAndValidationBefore
  with SavingEntityIdLongTests[PrintOrder]
  with CreatedUpdatedEntityTests[Long, PrintOrder]
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

  "getPngUrl" should "return URL that points to PNG" in {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val printOrder = PrintOrder(orderId = order.id).save()
    printOrder.getPngUrl should be(None)

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val pngUrl: Option[String] = printOrder.getPngUrl
    pngUrl.get should endWith("blob/files/egraphs/" + egraph.id + "/image/signing-origin-offset-0x0_global-width-2160px-v1.png")
    TestHelpers.getBlobFromTestBlobUrl(pngUrl.get).get.asByteArray.length should be(6458576)
  }

  "getFramedPrintImageData" should "return URL that points to framed-print sized image" in {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val printOrder = PrintOrder(orderId = order.id).save()
    printOrder.getFramedPrintImageData should be(None)

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val imageUrl = printOrder.getFramedPrintImageData.get._1
    imageUrl should endWith("blob/files/egraphs/" + egraph.id + "/framed-print/" + egraph.framedPrintFilename)
    TestHelpers.getBlobFromTestBlobUrl(imageUrl).get.asByteArray.length should be > (500000)
  }
}
