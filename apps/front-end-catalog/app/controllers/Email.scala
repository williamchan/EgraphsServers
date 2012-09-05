package controllers

import play.mvc.Controller

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def order_confirmation = {
    val emailLogoSrc = "../public/images/email-logo.jpg"
    val emailFacebookSrc = "../public/images/email-facebook.jpg"
    val emailTwitterSrc = "../public/images/email-twitter.jpg"
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
      hasPrintOrder = true,
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
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
    val emailLogoSrc = "../public/images/email-logo.jpg"
    val emailFacebookSrc = "../public/images/email-facebook.jpg"
    val emailTwitterSrc = "../public/images/email-twitter.jpg"
    views.frontend.html.email_account_verification(
      "http://www.egraphs.com/word",
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
  }

  def view_egraph = {
    val emailLogoSrc = "../public/images/email-logo.jpg"
    val emailFacebookSrc = "../public/images/email-facebook.jpg"
    val emailTwitterSrc = "../public/images/email-twitter.jpg"
    views.frontend.html.email_view_egraph(
      viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
      celebrityName = "Big Papi",
      recipientName = "Carlos Pena",
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
  }

  def confirm = {
    val emailLogoSrc = "../public/images/email-logo.jpg"
    val emailFacebookSrc = "../public/images/email-facebook.jpg"
    val emailTwitterSrc = "../public/images/email-twitter.jpg"
    views.frontend.html.email_account_confirmation(
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
    )
  }

  def mlb = {
    views.frontend.html.mlbmail()
  }
}
