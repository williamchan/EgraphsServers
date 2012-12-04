package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json.Json.toJson
import models.frontend.egraphs._
import models.frontend.forms.FormError
import models.frontend.egraphs.FulfilledEgraphViewModel
import models.frontend.account.{AccountRecoverForm, AccountPasswordResetForm, AccountSettingsForm}
import models.frontend.forms.Field
import play.api.mvc.Request
import play.data.DynamicForm
import helpers.DefaultImplicitTemplateParameters

/**
 * Test controller for viewing permutations of the account settings page.
 */
object Account extends Controller with DefaultImplicitTemplateParameters {

  val roles = Map ("other" -> OtherGalleryControl,
                        "admin" -> AdminGalleryControl,
                        "owner" -> OwnerGalleryControl)
  val pendingThumbnails = Map("landscape" -> "http://placehold.it/230x185",
                        "portrait" -> "http://placehold.it/170x225")
  val portraitPNG  = "http://localhost:9000/assets/images/width-350px.png"
  val landscapePNG = "http://localhost:9000/assets/images/width-510px.png"
  val fbAppId = "375687459147542"
  val fbAppSecret = "d38e551a2eb9b7c97fbb3bfb2896d426"

  def getSettings() = Action {
    Ok(views.html.frontend.account_settings(AccountSettingsFormFactory.default))
  }
  
  //You can test with this command:
  //  curl -d "param1=value1&param2=value2,value3" localhost:9000/Account/settings
  def postSettings() = Action {request =>
    printPostRequestData(request)
    //TODO: should i redirect them to the settings?
    Ok(views.html.frontend.account_settings(AccountSettingsFormFactory.default))
  }

  def getSubscribe() = Action {request =>
    Ok("Not sure why you are here :(")
  }

  def postSubscribe() = Action {request =>
    printPostRequestData(request)
    Ok("Subscribed = true")
  }

  def errors() = Action {
    Ok(views.html.frontend.account_settings(
      AccountSettingsFormFactory.errors,
      List("derp", "herp", "dont ever ever ever ever")
    ))
  }

  def getRecovery() = Action {
    Ok(views.html.frontend.account_recover(AccountRecoverForm(
      email = Field(name="email")
    )))
  }

  def postRecovery() = Action { request =>
    printPostRequestData(request)
    Ok(views.html.frontend.simple_message(
      "Password Recovery",
      """
      <p>
      Please check your email address for your account recovery information.
      </p>
      Thanks,
      <br>
      The team at Egraphs
      """
    ))
  }

  def getVerify() = Action {
    Ok(views.html.frontend.account_recover(AccountRecoverForm(
      email = Field(name="email")
    )))
  }

  def postVerify() = Action { request =>
    printPostRequestData(request)
    Ok(views.html.frontend.simple_message(
      "Account Verified",
      """
      <p>
      Your new password been confirmed. Continue on to the rest of the <a href="/">Egraph's</a> website.
      </p>
      Thanks,
      <br>
      The team at Egraphs
      """
    ))
  }

  def reset() = Action {
    Ok(views.html.frontend.account_password_reset(
      AccountPasswordResetForm(
        newPassword = Field(name="newPassword"),
        passwordConfirm = Field(name="passwordConfirm"),
        email = Field(name="email", values=List("will@egraphs.com")),
        secretKey = Field(name="secretKey", values=List("SECRETSAUCE"))
      )
    ))
  }

  def gallery(user: String, count: Int, role: String, pending: Int) = Action {
    val completed = makeEgraphs(user)
    val pending = makePendingEgraphs(user)

    val egraphs = pending ::: completed

    Ok(views.html.frontend.account_gallery(user, egraphs, roles(role)))
  }

  //Basic controller for testing privacy toggles on the gallery pages
  def privacy(orderId: Long, status: String) = Action {
    println("privacy status: " + status)
    Ok(toJson(Map("privacyStatus" -> status)))
  }

  private def printPostRequestData(request: play.api.mvc.Request[play.api.mvc.AnyContent]) {
    println("POST data")
    request.body.asFormUrlEncoded.foreach(map => map.foreach(println))
  }

  private def makePendingEgraphs(user: String) : List[PendingEgraphViewModel] = {
    List(
      PendingEgraphViewModel(
        orderStatus = "pending",
        orderDetails = new OrderDetails(
          orderNumber = 1,
          price = "$50.00",
          orderDate = "Nov 19th 2011 @ 2:30PM",
          statusText = "In progress",
          shippingMethod = "UPS",
          UPSNumber = "45Z343YHYU3343322J"),
        orderId = 1,
        orientation = "portrait",
        productUrl="egr.aphs/" + user +"/1",
        productTitle = "Telling Jokes",
        productPublicName = "Jimmy Fallon",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
          "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
          " volutpat at.",
        thumbnailUrl = pendingThumbnails("portrait")),
      PendingEgraphViewModel(
        orderStatus = "pending",
        orderDetails = new OrderDetails(
          orderNumber = 1,
          price = "$50.00",
          orderDate = "Jan 31st, 2012 @ 11:59PM",
          statusText = "In progress",
          shippingMethod = "UPS",
          UPSNumber = "45Z343YHYU3343322J"),
        orderId = 2,
        orientation = "landscape",
        productUrl="egr.aphs/" + user +"/1",
        productTitle = "You In Reverse",
        productPublicName = "Built To Spill",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
          "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
          " volutpat at.",
        thumbnailUrl = pendingThumbnails("landscape"))
    )
  }
  private def makeEgraphs(user: String): List[FulfilledEgraphViewModel]  = {
    List(
      FulfilledEgraphViewModel(
        viewEgraphUrl="www.egraphs.com/egraph/" + user + "1",
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        facebookShareLink = views.frontend.Utils.getFacebookShareLink(
          appId=fbAppId,
          picUrl = EgraphsAssets.at("/images/logo.png").url,
          name = "Chris Bosh",
          caption = "Winning the finals",
          description = "The story of this photo",
          link = "http://www.egraphs.com"
        ),
        twitterShareLink = views.frontend.Utils.getTwitterShareLink("http://www.egraphs.com/egraphs/1/",
          "Chris bosh gave me an eGraph!"),
        orderId = 3,
        orientation = "landscape",
        productUrl="egr.aphs/" + user +"/1",
        productTitle = "Man or Velociraptor?",
        productPublicName = "Chris Bosh",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = landscapePNG
      ),
      FulfilledEgraphViewModel(
        viewEgraphUrl="egr.aphs/" + user + "2",
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        facebookShareLink = "http://www.facebook.com",
        twitterShareLink = "http://www.twitter.com",
        orderId = 4,
        orientation = "portrait",
        productUrl="egr.aphs/" + user +"/2",
        productTitle = "King James",
        productPublicName = "Lebron-bron",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = portraitPNG
      ),
      FulfilledEgraphViewModel(
        viewEgraphUrl="egr.aphs/" + user + "2",
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        facebookShareLink = "www.facebook.com",
        twitterShareLink = "www.twitter.com",
        orderId = 5,
        orientation = "landscape",
        productUrl="egr.aphs/" + user +"/2",
        productTitle = "A cool bro",
        productPublicName = "Dwyane Wade",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = landscapePNG
      )
    )
  }
}

object AccountSettingsFormFactory {
  def default : AccountSettingsForm = {
    AccountSettingsForm(
      fullname = Field(name = "fullname", values = List("Will Chan")),
      username = Field(name = "username", values = List("willchan.m.odell")),
      email = Field(name = "email", values = List("will@egraphs.com")),
      oldPassword = Field(name = "oldPassword"),
      newPassword = Field(name = "newPassword"),
      passwordConfirm = Field(name = "passwordConfirm"),
      addressLine1 = Field(name = "address.line1", values = List("615 2nd Ave")),
      addressLine2 = Field(name = "address.line2", values = List("Suite 300")),
      city = Field(name = "city", values = List("Seattle")),
      state = Field(name = "state", values = List("WA")),
      postalCode = Field(name = "postalCode", values = List("98102")),
      galleryVisibility = Field(name = "galleryVisibility", values = List("private")),
      notice_stars = Field(name = "notice_stars", values = List("true")),
      generalErrors = List.empty[FormError]
    )
  }

  def errors : AccountSettingsForm = {
    val basic = default
    val error = Some(FormError("Get outta here this is no good!"))

    basic.copy(
      fullname = basic.fullname.copy(error=Option(FormError("fullname error"))),
      username = basic.username.copy(error=error),
      email = basic.email.copy(error=error),
      oldPassword = basic.oldPassword.copy(error=error),
      newPassword = basic.newPassword.copy(error=error),
      passwordConfirm = basic.oldPassword.copy(error=error),
      addressLine1 = basic.addressLine1.copy(error=error),
      addressLine2 = basic.addressLine2.copy(error=Option(FormError("derp"))),
      city = basic.city.copy(error=error),
      state = Field(name = "state", values = List("WA")),
      postalCode = basic.postalCode.copy(error=error),
      galleryVisibility = Field(name = "galleryVisibility", values = List("private")),
      notice_stars = Field(name = "notice_stars", values = List("true")),
      generalErrors = List(FormError("Derp"),FormError("Sclerp"),FormError("Lerp"))
    )
  }
}
