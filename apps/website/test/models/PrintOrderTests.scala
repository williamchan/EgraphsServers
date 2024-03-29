package models

import enums.EgraphState
import utils._
import services.AppConfig
import services.blobs.Blobs
import services.print.LandscapeFramedPrint
import Blobs.Conversions._

class PrintOrderTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with SavingEntityIdLongTests[PrintOrder]
  with CreatedUpdatedEntityTests[Long, PrintOrder]
  with DateShouldMatchers
  with DBTransactionPerTest {

  private def store = AppConfig.instance[PrintOrderStore]

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

  "getPngUrl" should "return URL that points to PNG" in new EgraphsTestApplication {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val printOrder = PrintOrder(orderId = order.id).save()
    printOrder.getPngUrl should be(None)

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val pngUrl: Option[String] = printOrder.getPngUrl
    pngUrl.get should endWith("blob/files/egraphs/" + egraph.id + "/image/signing-origin-offset-0x0_shadow-offset-3.00x3.00_global-width-" +
        LandscapeFramedPrint.targetEgraphWidth  + "px-v1.png")
    TestHelpers.getBlobFromTestBlobUrl(pngUrl.get).get.asByteArray.length should be(13737)
  }

  "getFramedPrintImageData" should "return URL that points to framed-print sized image" in new EgraphsTestApplication {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val printOrder = PrintOrder(orderId = order.id).save()
    printOrder.getFramedPrintImageData should be(None)

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val imageUrl = printOrder.getFramedPrintImageData.get._1
    imageUrl should endWith(egraph.framedPrintFilename)
    TestHelpers.getBlobFromTestBlobUrl(imageUrl).get.asByteArray.length should be > (500000)
  }

  "getStandaloneCertificateUrl" should "return URL that points to stand alone certificate image" in new EgraphsTestApplication {
    var egraph = TestData.newSavedEgraph()
    val order = egraph.order
    val product = order.product.copy(name = "Limited Edition: That's What's Up - 2012 World Series Win").save()
    val printOrder = PrintOrder(orderId = order.id).save()

    egraph = egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
    val imageUrl = printOrder.getStandaloneCertificateUrl.get
    imageUrl should endWith(egraph.standaloneCertPrintFilename)
    TestHelpers.getBlobFromTestBlobUrl(imageUrl).get.asByteArray.length should be > (500000)
  }
}
