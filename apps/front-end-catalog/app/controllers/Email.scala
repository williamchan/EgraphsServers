package controllers

import play.mvc.Controller

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def order_confirmation = {
    views.frontend.html.email_order_confirmation(
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
    )
  }

  def order_confirmation_text = {
    views.frontend.html.email_order_confirmation_text(
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
    )
  }

  def verify = {
    views.frontend.html.email_account_verification("http://www.egraphs.com/word")
  }

  def view_egraph = {
    views.frontend.html.email_view_egraph(
      viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
      celebrityName = "Big Papi",
      recipientName = "Carlos Pena"
    )
  }

  def confirm = {
    views.frontend.html.email_account_confirmation()
  }

  def mlb = {
    views.frontend.html.mlbmail()
  }

  def celebrity_welcome_email = {
    val publicName = "Rodney Strong"
    val email = "rstrong@fantastic.com"
    views.frontend.html.celebrity_welcome_email(
      celebrityName = publicName,
      celebrityEmail = email
      )
  }
}
