package controllers.api

import play.api.mvc.Controller
import sjson.json.Serializer
import play.data.validation._
import services.http.SafePlayParams.Conversions._
import play.libs.Codec
import services.http.{ControllerMethod, HttpCodes, OrderRequestFilters, CelebrityAccountRequestFilters}
import services.db.{TransactionSerializable, DBSession}
import actors.ProcessEgraphMessage
import akka.actor.ActorRef
import services.Time

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
  //TODO: PLAY20: take a look at PostCelebrityOrderApiEndpoint.postCelebrityOrder for some clues on how to do some of this, but there is more than that.
  def postEgraph(
    @Required signature: String,
    @Required audio: String,
    latitude: Option[Double] = None,
    longitude: Option[Double] = None,
    signedAt: String = "",
    skipBiometrics: Boolean = false /*todo(wchan): remove skipBiometrics parameter*/) =
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

                  val savedEgraph = order.newEgraph
                    .copy(latitude = latitude, longitude = longitude, signedAt = Time.timestamp(signedAt, Time.ipadDateFormat))
                    .withAssets(signature, message, Codec.decodeBASE64(audio))
                    .save()
                  val actorMessage = ProcessEgraphMessage(id = savedEgraph.id)
                  val responseJson = Serializer.SJSON.toJSON(Map("id" -> savedEgraph.id))
                  (actorMessage, responseJson)

                }
                else {
                  play.Logger.info("Dismissing the invalid postEgraph request")
                  if (Option(signature).isEmpty) play.Logger.info("Signature missing")
                  if (Option(audio).isEmpty) play.Logger.info("Audio missing")
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

