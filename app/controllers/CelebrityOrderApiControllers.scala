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
        val egraph = order.newEgraph(
          signature=signatureString.getBytes("UTF-8"),
          audio=Codec.decodeBASE64(audioString)
        )
        val id = egraph.save().id
        Serializer.SJSON.toJSON(Map("id" -> id))
        
      case _ =>
        Error("Valid \"signature\" and \"audio\" parameters were not provided.")
    }
  }
}