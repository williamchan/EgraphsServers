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
import services.http.OptionParams.Conversions._

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
  def postEgraph(
    @Required signature: String,
    @Required audio: String,
    skipBiometrics: Boolean = false) =
  {
    if (validationErrors.isEmpty) {
      val message = request.params.getOption("message")
      celebFilters.requireCelebrityAccount { (account, celebrity) =>
        orderFilters.requireOrderIdOfCelebrity(celebrity.id) { order =>
          EgraphFulfillmentHandler(signature, audio, order, celebrity, mail, skipBiometrics, message).execute()
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
  case class EgraphFulfillmentHandler(
    signature: String,
    audio: String,
    order: Order,
    celebrity: Celebrity,
    mail: Mail,
    skipBiometrics: Boolean = false,
    message: Option[String] = None)
  {
    def execute() = {
      play.Logger.info("Processing eGraph submission for Order #" + order.id)

      val savedEgraph = order
        .newEgraph
        .withAssets(signature, message, Codec.decodeBASE64(audio))
        .save()

      val egraphToVerify = if (skipBiometrics) savedEgraph.withNiceBiometricServices else savedEgraph
      val testedEgraph = egraphToVerify.verifyBiometrics.saveWithoutAssets()

      if (testedEgraph.state == Verified) {
        sendEgraphSignedMail(testedEgraph)
      }

      // Serialize the egraph ID
      Serializer.SJSON.toJSON(Map("id" -> testedEgraph.id))
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
  }
}
