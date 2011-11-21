package controllers

import play.mvc.Controller
import play.libs.Codec
import sjson.json.Serializer

/**
 * Handles requests for queries against a celebrity for his orders.
 */
object CelebrityOrderApiControllers extends Controller
  with RequiresAuthenticatedAccount
  with RequiresCelebrityId
  with RequiresCelebrityOrderId
  with DBTransaction
{
  
  def postEgraph(signature: Option[String], audio: Option[String]) = {
    (signature, audio) match {
      case (Some(signatureString), Some(audioString)) =>
        val egraphId = order
          .newEgraph
          .save(signatureString, Codec.decodeBASE64(audioString))
          .id

        Serializer.SJSON.toJSON(Map("id" -> egraphId))
        
      case _ =>
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }
}