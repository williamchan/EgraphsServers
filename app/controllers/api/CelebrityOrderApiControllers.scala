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
import models._
import services.voice.{VoiceBiometricsClient, VBGBiometricServices}
import services.signature.XyzmoBiometricServices
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub

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

        val passesBiometricVerification = verifyBiometrics(egraph)
        if (passesBiometricVerification) {
          // demo code (refactor it later):
          createEgraphImage(egraphId, signatureString)

          // Send e-mail that the eGraph is complete. Move this to post-authentication
          // when possible.
          sendEgraphSignedMail(egraph)
        }

        // Serialize the egraph ID
        Serializer.SJSON.toJSON(Map("id" -> egraphId))

      case _ =>
        play.Logger.info("Dismissing the invalid request")
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }

  private def verifyBiometrics(egraph: Egraph): Boolean = {
    val isVoiceVerified = verifyVoice(egraph)
    val isSignatureVerified = verifySignature(egraph)

    val egraphState = {
      if (isSignatureVerified && isVoiceVerified) Verified
      else if (!isSignatureVerified && isVoiceVerified) RejectedSignature
      else if (isSignatureVerified && !isVoiceVerified) RejectedVoice
      else RejectedBoth
    }
    egraph.withState(egraphState).saveWithoutAssets()

    egraphState == Verified
  }

  private def verifySignature(egraph: Egraph): Boolean = {
    val signatureToVerify: String = egraph.assets.signature
    val verifyUserResponse = XyzmoBiometricServices.verifyUser(userId = celebrity.id.toString, signatureToVerify)
    verifyUserResponse.getOkInfo.getVerifyResult == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch
  }

  private def verifyVoice(egraph: Egraph): Boolean = {
    val startVerificationRequest = VBGBiometricServices.sendStartVerificationRequest(celebrity.id.toString)
    val transactionId = startVerificationRequest.getResponseValue(VBGBiometricServices._transactionId)

    val sendVerifySampleRequest = VBGBiometricServices.sendVerifySampleRequest(transactionId, blobLocation = "egraphs/" + egraph.id + "/audio.wav")
    val verificationScore = sendVerifySampleRequest.getResponseValue(VoiceBiometricsClient.score)
    val verificationResult = sendVerifySampleRequest.getResponseValue(VoiceBiometricsClient.success)

    // Why do we need to do this?
    VBGBiometricServices.sendFinishVerifyTransactionRequest(transactionId, verificationResult, verificationScore)

    verificationResult == "true"
  }

  private def createEgraphImage(egraphId: Long, signatureString: String) {
    val photoImage: BufferedImage = ImageIO.read(new File("test/files/kapler.JPG"))
    val signatureImage = ImageUtil.createSignatureImage(signatureString)
    val egraphImage: BufferedImage = ImageUtil.createEgraphImage(signatureImage, photoImage, 0, 0)
    val byteOs = new ByteArrayOutputStream()
    ImageIO.write(egraphImage, "JPG", byteOs)
    val bytes = byteOs.toByteArray
    Blobs.put("egraphs/" + egraphId + "/egraph.jpg", bytes)
  }

  private def sendEgraphSignedMail(egraph: Egraph) = {
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