package controllers

import play.mvc.Controller

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def index = {
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
      deliveredyDate = "June 19, 2012",
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc
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
    val emailLogoSrc = "../public/images/email-logo.jpg"
    val emailFacebookSrc = "../public/images/email-facebook.jpg"
    val emailTwitterSrc = "../public/images/email-twitter.jpg"
    val egraphImageSrc = "../public/images/hamilton-egraph.png"
    views.frontend.html.email_mlb(
      emailLogoSrc = emailLogoSrc,
      emailFacebookSrc = emailFacebookSrc,
      emailTwitterSrc = emailTwitterSrc,
      egraphImageSrc = egraphImageSrc
    )
  }
}
