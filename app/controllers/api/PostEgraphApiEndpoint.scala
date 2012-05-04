package controllers.api

import play.mvc.Controller
import sjson.json.Serializer
import play.data.validation._
import services.http.OptionParams.Conversions._
import play.libs.Codec
import services.http.{ControllerMethod, HttpCodes, OrderRequestFilters, CelebrityAccountRequestFilters}
import services.db.{TransactionSerializable, DBSession}
import actors.ProcessEgraphMessage
import akka.actor.ActorRef

private[controllers] trait PostEgraphApiEndpoint { this: Controller =>
  protected def egraphActor: ActorRef
  protected def dbSession: DBSession
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
  {
    controllerMethod(openDatabase=false) {
      // Get result of DB transaction that processes the request
      val transactionResult = dbSession.connected(TransactionSerializable) {

        celebFilters.requireCelebrityAccount {
          (account, celebrity) =>
            orderFilters.requireOrderIdOfCelebrity(celebrity.id) {
              order =>

                // validate signature for issue #104

                if (validationErrors.isEmpty) {
                  val message = request.params.getOption("message")

                  val savedEgraph = order.newEgraph.withAssets(
                    signature,
                    message,
                    Codec.decodeBASE64(audio)
                  ).save()
                  val actorMessage = ProcessEgraphMessage(id = savedEgraph.id, skipBiometrics = skipBiometrics)
                  val responseJson = Serializer.SJSON.toJSON(Map("id" -> savedEgraph.id))
                  (actorMessage, responseJson)

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
      }

      // Route the result to an actor that performs further processing
      transactionResult match {
        case (message: ProcessEgraphMessage, json: String) =>
          egraphActor ! message

          json

        case otherResult =>
          otherResult
      }
    }
  }
}

