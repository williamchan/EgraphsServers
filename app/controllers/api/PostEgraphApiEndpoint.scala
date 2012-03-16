package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import models._
import play.data.validation._
import services.http.OptionParams.Conversions._
import play.libs.Codec
import services.http.{HttpCodes, OrderRequestFilters, CelebrityAccountRequestFilters}

private[controllers] trait PostEgraphApiEndpoint { this: Controller =>
  protected def celebFilters: CelebrityAccountRequestFilters
  protected def orderFilters: OrderRequestFilters

  /**
   * Posts a signed egraph from a celebrity.
   *
   * See [[https://egraphs.jira.com/wiki/display/DEV/API+Endpoints the json spec]] for more info
   * about the params.
   */
  def postEgraph(
    @Required signature: String,
    @Required audio: String,
    skipBiometrics: Boolean = false) =
  {
    if (validationErrors.isEmpty) {
      val message = request.params.getOption("message")

      celebFilters.requireCelebrityAccount { (account, celebrity) =>
        orderFilters.requireOrderIdOfCelebrity(celebrity.id) { order =>
          play.Logger.info("Processing Egraph submission for Order #" + order.id)

          val savedEgraph = order.newEgraph.withAssets(
            signature,
            message,
            Codec.decodeBASE64(audio)
          ).save()
          
          val egraphToTest = if (skipBiometrics) savedEgraph.withYesMaamBiometricServices else savedEgraph
          
          val testedEgraph = egraphToTest.verifyBiometrics.save()
          
          if (testedEgraph.state == EgraphState.Verified) {
            order.sendEgraphSignedMail()
          }
          
          Serializer.SJSON.toJSON(Map("id" -> testedEgraph.id))
        }
      }
    }
    else {
      play.Logger.info("Dismissing the invalid request")
      Error(HttpCodes.MalformedEgraph, "")
    }
  }
}

