package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import models._
import play.data.validation._
import services.http.{OrderRequestFilters, CelebrityAccountRequestFilters}
import services.Mail
import services.http.OptionParams.Conversions._
import play.libs.Codec

/**
 * Handles requests for queries against a celebrity for his orders.
 */
private[controllers] trait PostEgraphApiEndpoint { this: Controller =>

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
          play.Logger.info("Processing eGraph submission for Order #" + order.id)

          val savedEgraph = order.newEgraph.withAssets(
            signature,
            message,
            Codec.decodeBASE64(audio)
          ).save()
          
          val egraphToTest = if (skipBiometrics) savedEgraph.withNiceBiometricServices else savedEgraph
          
          val testedEgraph = egraphToTest.verifyBiometrics
          
          if (testedEgraph.state == EgraphState.Verified) {
            order.sendEgraphSignedMail()
          }
          
          Serializer.SJSON.toJSON(Map("id" -> testedEgraph.id))
        }
      }
    }
    else {
      play.Logger.info("Dismissing the invalid request")
      Error(5000, "Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }
}

