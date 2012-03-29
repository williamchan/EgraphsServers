package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import models._
import play.data.validation._
import services.http.OptionParams.Conversions._
import play.libs.Codec
import services.http.{ControllerMethod, HttpCodes, OrderRequestFilters, CelebrityAccountRequestFilters}

private[controllers] trait PostEgraphApiEndpoint { this: Controller =>
  protected def controllerMethod: ControllerMethod
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
  controllerMethod() {
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

          savedEgraph.assets.initMasterImage()
          
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
      play.Logger.info("Dismissing the invalid postEgraph request")
      play.Logger.info("Signature length was " + (if (signature != null) signature.length() else "[signature missing]"))
      play.Logger.info("Audio length was " + (if (audio != null) audio.length() else "[audio missing]"))
      play.Logger.info("ValidationErrors:" + validationErrors.map(pair => "" + pair._1 + " " + pair._2.message).mkString(". "))

      Error(HttpCodes.MalformedEgraph, "")
    }
  }
}

