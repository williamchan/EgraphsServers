package controllers

import play.api._
import play.api.mvc._

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def orderConfirmation = Action {
    Ok(views.html.frontend.email_order_confirmation(
      buyerName = "Joshua Johnson",
      recipientName = "Carlos Pena",
      recipientEmail = "carlos@egraphs.com",
      celebrityName = "David Price",
      productName = "First MLB Victory",
      orderDate = "June 12, 2012",
      orderId = "2387354",
      pricePaid = "$50.00",
      deliveredByDate = "June 19, 2012",
      faqHowLongLink = "https://www.egraphs.com/faq#how-long",
      hasPrintOrder = true
    ))
  }

  def orderConfirmationText = Action {
    Ok(views.html.frontend.email_order_confirmation_text(
      buyerName = "Joshua Johnson",
      recipientName = "Carlos Pena",
      recipientEmail = "carlos@egraphs.com",
      celebrityName = "David Price",
      productName = "First MLB Victory",
      orderDate = "June 12, 2012",
      orderId = "2387354",
      pricePaid = "$50.00",
      deliveredByDate = "June 19, 2012",
      faqHowLongLink = "https://www.egraphs.com/faq#how-long",
      hasPrintOrder = true
    ))
  }

  def verify = Action {
    Ok(views.html.frontend.email_account_verification("http://www.egraphs.com/word"))
  }

  def viewEgraph = Action {
    Ok(views.html.frontend.email_view_egraph(
      viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
      celebrityName = "Big Papi",
      recipientName = "Carlos Pena"
    ))
  }

  def confirm = Action {
    Ok(views.html.frontend.email_account_confirmation())
  }

//  def mlbStatic = Action {
////    Redirect(routes.RemoteAssets.at("html/email_mlb_marketing_static.html"))
//    Ok(views.frontend.mlbmail())
//  }
//
//  def celebrity_welcome_email = {
//    val publicName = "Rodney Strong"
//    val email = "rstrong@fantastic.com"
//    Ok(views.frontend.html.celebrity_welcome_email(
//      celebrityName = publicName,
//      celebrityEmail = email
//      ))
//  }
}
