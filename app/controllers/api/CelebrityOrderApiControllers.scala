package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import controllers.{DBTransaction, RequiresCelebrityOrderId, RequiresCelebrityId, RequiresAuthenticatedAccount}
import org.apache.commons.mail.SimpleEmail
import play.libs.Codec
import models._
import services.voice.{VoiceBiometricsClient, VBGBiometricServices}
import services.signature.XyzmoBiometricServices
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub
import models.{Verified, Celebrity, Order, Egraph}

/**
 * Handles requests for queries against a celebrity for his orders.
 */
object CelebrityOrderApiControllers extends Controller
with RequiresAuthenticatedAccount
with RequiresCelebrityId
with RequiresCelebrityOrderId
with DBTransaction {

  def postEgraph(signature: Option[String], audio: Option[String], skipBiometrics: Boolean = false) = {
    (signature, audio) match {
      case (Some(signatureString), Some(audioString)) =>
        EgraphFulfillmentHandler(signatureString, audioString, order, celebrity, skipBiometrics).execute()

      case _ =>
        play.Logger.info("Dismissing the invalid request")
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }

  case class EgraphFulfillmentHandler(signature: String,
                                      audio: String,
                                      order: Order,
                                      celebrity: Celebrity,
                                      skipBiometrics: Boolean = false) {
    def execute() = {
      play.Logger.info("Processing eGraph submission for Order #" + order.id)

      val egraph = order
        .newEgraph
        .save(signature, Codec.decodeBASE64(audio))

      val egraphId = egraph.id

      if (skipBiometrics || verifyBiometrics(egraph)) {
        // Send e-mail that the eGraph is complete. Move this to post-authentication
        // when possible.
        sendEgraphSignedMail(egraph)
      }

      // Serialize the egraph ID
      Serializer.SJSON.toJSON(Map("id" -> egraphId))
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

      libs.Mail.send(email)
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
      val sdc = XyzmoBiometricServices.getSignatureDataContainerFromJSON(signatureToVerify).getGetSignatureDataContainerFromJSONResult
      val verifyUserResponse = XyzmoBiometricServices.verifyUser(userId = celebrity.getXyzmoUID(), sdc)
      val verifyResult = verifyUserResponse.getOkInfo.getVerifyResult
      println("Signature verification result for egraph " + egraph.id.toString + ": " + verifyResult.toString)
      verifyResult == WebServiceBiometricPartStub.VerifyResultEnum.VerifyMatch
    }

    private def verifyVoice(egraph: Egraph): Boolean = {
      val startVerificationRequest = VBGBiometricServices.sendStartVerificationRequest(celebrity.id.toString)
      val transactionId = startVerificationRequest.getResponseValue(VBGBiometricServices._transactionId)

      val sendVerifySampleRequest = VBGBiometricServices.sendVerifySampleRequest(transactionId, wavBinary = egraph.assets.audio.toArray)
      val errorCode = sendVerifySampleRequest.getResponseValue(VoiceBiometricsClient.errorcode)
      val verificationResult = sendVerifySampleRequest.getResponseValue(VoiceBiometricsClient.success)
      val verificationScore = sendVerifySampleRequest.getResponseValue(VoiceBiometricsClient.score)

      // Question for VBG: Why do we need to do this?
      VBGBiometricServices.sendFinishVerifyTransactionRequest(transactionId, verificationResult, verificationScore)

      println("Voice verification result for egraph " + egraph.id.toString + ": " + verificationResult + " (" + errorCode + " )")

      errorCode == "0" && verificationResult == "true"
    }
  }

}