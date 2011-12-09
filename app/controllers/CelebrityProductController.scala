package controllers

import play.mvc.Controller
import models.{Celebrity, Product}
import libs.Utils

import play.data.validation._

/**
 * Serves pages relating to a particular product of a celebrity.
 */
object CelebrityProductController extends Controller
  with DBTransaction
  with RequiresCelebrityName
  with RequiresCelebrityProductName
{
  
  def index = {
    // Get any errors that came from an unsuccessful buy
    val errorFields = Option(flash.get("errors")).map(errString => errString.split(',').toList)

    // Render the page
    views.Application.html.product(celebrity, product, errorFields)
  }

  def buy(recipientName: String,
          recipientEmail: String,
          buyerName: String,
          buyerEmail: String,
          stripeTokenId: String,
          desiredText: Option[String],
          personalNote: Option[String]) =
  {
    import Validation.required
    required("Recipient name", recipientName)
    required("Recipient E-mail address", recipientEmail)
    required("Buyer name", buyerName)
    required("Buyer E-mail address", buyerEmail)
    required("stripeTokenId", stripeTokenId)

    if (validationErrors.isEmpty) {

    } else {
      // Redirect back to the index page, providing field errors via the flash scope.
      val fieldNames = validationErrors.map { case (fieldName, _) => fieldName }
      val errorString = fieldNames.mkString(",")

      flash += ("errors" -> errorString)

      Redirect(indexUrl(celebrity, product).url, false)
    }
  }

  /**
   * Reverses the product index url for a particular celebrity and product
   */
  def indexUrl(celebrity:Celebrity, product:Product) = {
    val params: Map[String, AnyRef] = Map(
      "celebrityUrlSlug" -> celebrity.urlSlug.get,
      "productUrlSlug" -> product.urlSlug
    )

    Utils.lookupUrl("CelebrityProductController.index", params)
  }
}