package controllers.api

import play.mvc.Controller
import play.libs.Codec
import sjson.json.Serializer
import controllers.{DBTransaction, RequiresCelebrityOrderId, RequiresCelebrityId, RequiresAuthenticatedAccount}
import java.awt.image.BufferedImage
import libs.{ImageUtil, Blobs}
import javax.imageio.ImageIO
import java.io.{File, ByteArrayOutputStream}

/**
 * Handles requests for queries against a celebrity for his orders.
 */
object CelebrityOrderApiControllers extends Controller
with RequiresAuthenticatedAccount
with RequiresCelebrityId
with RequiresCelebrityOrderId
with DBTransaction {

  def postEgraph(signature: Option[String], audio: Option[String]) = {
    (signature, audio) match {
      case (Some(signatureString), Some(audioString)) =>
        play.Logger.info("Processing the request as if valid")
        val egraphId = order
          .newEgraph
          .save(signatureString, Codec.decodeBASE64(audioString))
          .id

        Blobs.put("egraphs/" + egraphId + "/signature.json", signatureString.getBytes)

        // demo code (refactor it later):
        val photoImage: BufferedImage = ImageIO.read(new File("test/files/kapler.JPG"))
        val signatureImage = ImageUtil.createSignatureImage(signatureString)
        val egraphImage: BufferedImage = ImageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
        val byteOs = new ByteArrayOutputStream()
        ImageIO.write(egraphImage, "JPG", byteOs)
        val bytes = byteOs.toByteArray
        Blobs.put("egraphs/" + egraphId + "/egraph.jpg", bytes)

        Serializer.SJSON.toJSON(Map("id" -> egraphId))

      case _ =>
        play.Logger.info("Dismissing the invalid request")
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }
}