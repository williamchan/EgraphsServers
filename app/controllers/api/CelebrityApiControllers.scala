package controllers.api

import controllers.{RequiresAuthenticatedAccount, RequiresCelebrityId, DBTransaction}
import models._
import models.Order.FindByCelebrity.Filters
import play.mvc.Controller
import sjson.json.Serializer

/**
 * Controllers that handle direct API requests for celebrity resources.
 */
object CelebrityApiControllers extends Controller
with RequiresAuthenticatedAccount
with RequiresCelebrityId
with DBTransaction {

  def getCelebrity = {
    Serializer.SJSON.toJSON(celebrity.renderedForApi)
  }

  def postEnrollmentSample(signature: Option[String], audio: Option[String]) = {
    (signature, audio) match {
      case (Some(signatureString), Some(audioString)) =>
        val openEnrollmentBatch: Option[EnrollmentBatch] = celebrity.getOpenEnrollmentBatch()
        // TODO: is there a nice way to use a match here instead of if-else?

        if (openEnrollmentBatch.isEmpty) {
          val enrollmentBatch = EnrollmentBatch(celebrityId = celebrity.id).save()
          val enrollmentSamplePair = enrollmentBatch.addEnrollmentSample(signatureString, audioString)
          Serializer.SJSON.toJSON(Map("id" -> enrollmentSamplePair._1.id, "batch_complete" -> enrollmentSamplePair._2))

        } else if (!openEnrollmentBatch.get.isBatchComplete) {
          val enrollmentSamplePair = openEnrollmentBatch.get.addEnrollmentSample(signatureString, audioString)
          Serializer.SJSON.toJSON(Map("id" -> enrollmentSamplePair._1.id, "batch_complete" -> enrollmentSamplePair._2))

        } else {
          // TODO(wchan): Should we reject data if this situation ever arises?
          Error("Open enrollment batch already exists and is awaiting enrollment attempt. No further enrollment samples required now.")
        }

      case _ =>
        play.Logger.info("Dismissing the invalid request")
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }

  def getOrders(signerActionable: Option[Boolean]) = {
    signerActionable match {
      case None =>
        Error("Please pass in signerActionable=true")

      case Some(false) =>
        Error("signerActionable=false is not a supported filter")

      case _ =>
        val filters = Nil ++ (for (trueValue <- signerActionable) yield Filters.ActionableOnly)
        val orders = Order.FindByCelebrity(celebrity.id, filters: _*)
        val ordersAsMaps = orders.map(order => order.renderedForApi)

        Serializer.SJSON.toJSON(ordersAsMaps)
    }
  }
}