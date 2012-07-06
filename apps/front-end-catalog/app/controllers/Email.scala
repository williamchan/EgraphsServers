package controllers

import play.mvc.Controller

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def index = {
    views.frontend.html.email_order_confirmation(
      buyerName = "Joshua Johnson",
      recipientName = "Carlos Pena",
      recipientEmail = "carlos@egraphs.com",
      celebrityName = "David Price",
      productName = "First MLB Victory",
      orderDate = "June 12, 2012",
      orderId = "2387354",
      pricePaid = "$50.00",
      deliveredyDate = "June 19, 2012"
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
}
