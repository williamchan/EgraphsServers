package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import play.libs.Codec
import models._
import services.voice.{VoiceBiometricsClient, VBGBiometricServices}
import services.signature.XyzmoBiometricServices
import com.xyzmo.wwww.biometricserver.WebServiceBiometricPartStub
import models.{Celebrity, Order, Egraph}
import controllers._
import play.data.validation._
import org.apache.commons.mail.{HtmlEmail, SimpleEmail}
import services.http.{OrderRequestFilters, CelebrityAccountRequestFilters}
import services.Mail
import EgraphState._

/**
 * Handles requests for queries against a celebrity for his orders.
 */
private[controllers] trait PostEgraphApiEndpoint { this: Controller =>
  import PostEgraphApiEndpoint.EgraphFulfillmentHandler

  //
  // Abstract members
  //
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def orderFilters: OrderRequestFilters
  protected def mail: Mail

  //
  // Controller members
  //
  def postEgraph(@Required signature: String, @Required audio: String, skipBiometrics: Boolean = false) = {
    if (validationErrors.isEmpty) {
      celebFilters.requireCelebrityAccount { (account, celebrity) =>
        orderFilters.requireOrderIdOfCelebrity(celebrity.id) { order =>
          EgraphFulfillmentHandler(signature, audio, order, celebrity, mail, skipBiometrics).execute()
        }
      }
    }
    else {
      play.Logger.info("Dismissing the invalid request")
      Error(5000, "Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }
}

/**
 * Handles requests for queries against a celebrity for his orders.
 */
object PostEgraphApiEndpoint {
  case class EgraphFulfillmentHandler(signature: String,
                                      audio: String,
                                      order: Order,
                                      celebrity: Celebrity,
                                      mail: Mail,
                                      skipBiometrics: Boolean = false) {
    def execute() = {
      play.Logger.info("Processing eGraph submission for Order #" + order.id)

      val egraph = order
        .newEgraph
        .save(signature, Codec.decodeBASE64(audio))

      val egraphId = egraph.id

      if (verifyBiometrics(egraph, skipBiometrics)) {
        // Send e-mail that the eGraph is complete. Move this to post-authentication
        // when possible.
        sendEgraphSignedMail(egraph)
      }

      // Serialize the egraph ID
      Serializer.SJSON.toJSON(Map("id" -> egraphId))
    }

    private def sendEgraphSignedMail(egraph: Egraph) = {
      val email = new HtmlEmail()
      val recipient = order.recipient
      val linkActionDefinition = WebsiteControllers.lookupGetEgraph(order.id)
      linkActionDefinition.absolute()

      email.setFrom(celebrity.urlSlug.get + "@egraphs.com", celebrity.publicName.get)
      email.addTo(recipient.account.email, order.recipientName)
      email.addReplyTo("noreply@egraphs.com")
      email.setSubject("I just finished signing your eGraph")
      email.setMsg(
        views.Application.html.egraph_signed_email(
          celebrity,
          order.product,
          order,
          egraph,
          linkActionDefinition.url
        ).toString().trim()
      )

      mail.send(email)
    }

    private def verifyBiometrics(egraph: Egraph, skipBiometrics: Boolean): Boolean = {
      if (skipBiometrics) {
        egraph.withState(Verified).saveWithoutAssets()
        return true
      }

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
      println("Signature verification result for egraph " + egraph.id.toString + ": " + verifyResult.toString + " " + verifyUserResponse.getOkInfo.getScore.toString + "%")
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
