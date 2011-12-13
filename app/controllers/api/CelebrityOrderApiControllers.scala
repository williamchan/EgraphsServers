package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import controllers.{DBTransaction, RequiresCelebrityOrderId, RequiresCelebrityId, RequiresAuthenticatedAccount}
import java.awt.image.BufferedImage
import libs.{ImageUtil, Blobs}
import javax.imageio.ImageIO
import java.io.{File, ByteArrayOutputStream}
import org.apache.commons.mail.SimpleEmail
import play.libs.{Mail, Codec}
import models.Egraph

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
        val egraph = order
          .newEgraph
          .save(signatureString, Codec.decodeBASE64(audioString))
        val egraphId = egraph.id
        
        Blobs.put("egraphs/" + egraphId + "/signature.json", signatureString.getBytes)

        // demo code (refactor it later):
        val photoImage: BufferedImage = ImageIO.read(new File("test/files/kapler.JPG"))
        val signatureImage = ImageUtil.createSignatureImage(signatureString)
        val egraphImage: BufferedImage = ImageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
        val byteOs = new ByteArrayOutputStream()
        ImageIO.write(egraphImage, "JPG", byteOs)
        val bytes = byteOs.toByteArray
        Blobs.put("egraphs/" + egraphId + "/egraph.jpg", bytes)

        // Send e-mail that the eGraph is complete. Move this to post-authentication
        // when possible.
        sendEgraphSignedMail(egraph)

        // Serialize the egraph ID
        Serializer.SJSON.toJSON(Map("id" -> egraphId))

      case _ =>
        play.Logger.info("Dismissing the invalid request")
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }

  def sendEgraphSignedMail(egraph: Egraph) = {
    val email = new SimpleEmail()
    val recipient = order.recipient

    email.setFrom(celebrity.urlSlug.get + "@egraphs.com", celebrity.publicName.get)
    email.addTo(recipient.account.email, recipient.name)
    email.addReplyTo("noreply@egraphs.com")
    email.setSubject("I just finished signing your eGraph")
    email.setMsg(
      views.Application.html.egraph_signed_email(
        recipient,
        celebrity,
        order.product,
        order,
        egraph
      ).toString().trim()
    )

    Mail.send(email)
  }

}