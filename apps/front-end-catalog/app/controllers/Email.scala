package controllers

import play.api._
import play.api.mvc._
import models.frontend.email._
import egraphs.playutils.MaleGrammar

/**
 * Permutations of Emails.
 */
object Email extends Controller {

  def celebrityRequest = Action {
    Ok(views.html.frontend.email.celebrity_request(
      CelebrityRequestEmailViewModel(
        requesterEmail = "awesomeemail@gmail.com",
        requestedStar = "Celebrity Jane"
      )
    ))
  }

  def celebrityWelcome = Action {
    val publicName = "Rodney Strong"
    val email = "rstrong@fantastic.com"
    Ok(views.html.frontend.email.celebrity_welcome(
      CelebrityWelcomeEmailViewModel(
        celebrityName = publicName,
        celebrityEmail = email,
        appPlistUrl = "//path/to/app"
      )
    ))
  }

  def confirm = Action {
    Ok(views.html.frontend.email.account_confirmation())
  }

  def enrollmentComplete = Action {
    Ok(views.html.frontend.email.enrollment_complete(
      EnrollmentCompleteEmailViewModel(
        celebrityName = "Celebrity Jane",
        videoAssetIsDefined = true,
        celebrityEnrollmentStatus = "Enrolled",
        timeEnrolled = "March 4, 2013"
      )
    ))
  }

  def mlbStatic = Action {
    Redirect(routes.EgraphsAssets.at("html/email_mlb_marketing_static.html"))
  }

  def orderConfirmation = Action {
    Ok(views.html.frontend.email.order_confirmation(
      OrderConfirmationEmailViewModel(
        buyerName = "Joshua Johnson",
        buyerEmail = "joshua@johnson.com",
        recipientName = "Carlos Pena",
        recipientEmail = "carlos@egraphs.com",
        celebrityName = "David Price",
        celebrityGrammar = MaleGrammar,
        productName = "First MLB Victory",
        orderDate = "June 12, 2012",
        orderId = "2387354",
        pricePaid = "$50.00",
        deliveredByDate = "June 19, 2012",
        faqHowLongLink = "https://www.egraphs.com/faq#how-long",
        messageToCelebrity = "Hey David Price, I'm your biggest fan, thanks for being awesome!",
        maybePrintOrderShippingAddress = Some("1234 Cherry Lane New York, NY 12345")
      )
    ))
  }

  def resetPassword = Action {
    val email = "cooluser@fantastic.com"
    val resetPasswordUrl = "http://www.egraphs.com/cool"
    Ok(views.html.frontend.email.reset_password(
      ResetPasswordEmailViewModel(
        email = email,
        resetPasswordUrl = resetPasswordUrl
      )
    ))
  }

  def siteShutdown = Action {
    Ok(views.html.frontend.email.site_shutdown("Herp Derpson", "http://egraphs.com"))
  }

  def verify = Action {
    Ok(views.html.frontend.email.account_verification(
      AccountVerificationEmailViewModel(
        verifyPasswordUrl = "http://www.egraphs.com/word"
      )
    ))
  }

  def viewEgraph = Action {
    Ok(views.html.frontend.email.view_egraph(
      RegularViewEgraphEmailViewModel(
        viewEgraphUrl = "https://www.egraphs.com/gallery/carlosdiaz/davidortiz1",
        celebrityPublicName = "Big Papi",
        recipientName = "Carlos Pena"
      ),
      CouponModuleEmailViewModel(
        discountAmount = 15,
        code = "XXXXXXXX",
        daysUntilExpiration = 7
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
      ),
      CouponModuleEmailViewModel(
        discountAmount = 15,
        code = "XXXXXXXX",
        daysUntilExpiration = 7
      )
    ))
  }
}