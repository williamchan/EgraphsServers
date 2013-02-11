package controllers

import play.api._
import play.api.mvc._
import models.frontend.email._

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def orderConfirmation = Action {
    Ok(views.html.frontend.email.order_confirmation(
      OrderConfirmationEmailViewModel(
        buyerName = "Joshua Johnson",
        buyerEmail = "joshua@johnson.com",
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
      )
    ))
  }

  def orderConfirmationText = Action {
    Ok(views.txt.frontend.email.order_confirmation(
      OrderConfirmationEmailViewModel(
        buyerName = "Joshua Johnson",
        buyerEmail = "joshua@johnson.com",
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
      )
    ))
  }

  def verify = Action {
    Ok(views.html.frontend.email.account_verification("http://www.egraphs.com/word"))
  }

  def viewEgraph = Action {
    Ok(views.html.frontend.email.view_egraph(
      RegularViewEgraphEmailViewModel(
        viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
        celebrityPublicName = "Big Papi",
        recipientName = "Carlos Pena"
      )
    ))
  }

  def viewGiftEgraph = Action {
    Ok(views.html.frontend.email.view_egraph(
      GiftViewEgraphEmailViewModel(
        viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
        celebrityPublicName = "Big Papi",
        recipientName = "Carlos Pena",
        buyerName = "Mr. Generous"
      )
    ))
  }

  def confirm = Action {
    Ok(views.html.frontend.email.account_confirmation())
  }

  def mlbStatic = Action {
    Redirect(routes.EgraphsAssets.at("html/email_mlb_marketing_static.html"))
  }
  
  def celebrityWelcome = Action {
    val publicName = "Rodney Strong"
    val email = "rstrong@fantastic.com"
    Ok(views.html.frontend.email.celebrity_welcome(
      celebrityName = publicName,
      celebrityEmail = email,
      appPlistUrl = "//path/to/app"
    ))
  }

  def resetPassword = Action {
    val email = "cooluser@fantastic.com"
    val resetPasswordUrl = "http://www.egraphs.com/cool"
    Ok(views.html.frontend.email.reset_password(
      email = email,
      resetPasswordUrl = resetPasswordUrl
    ))
  }
}