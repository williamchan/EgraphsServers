package controllers

import play.mvc.Controller
import models.frontend.account.AccountSettingsForm
import models.frontend.forms.{FormError, Field}
import models.frontend.egraphs._
import models.frontend.forms.FormError
import models.frontend.egraphs.FulfilledEgraphViewModel
import models.frontend.account.AccountSettingsForm
import models.frontend.forms.Field

object Account extends Controller {

  val roles = Map ("other" -> OtherGalleryControl,
                        "admin" -> AdminGalleryControl,
                        "owner" -> OwnerGalleryControl)
  val pendingThumbnails = Map("landscape" -> "http://placehold.it/230x185",
                        "portrait" -> "http://placehold.it/170x225")
  val portraitPNG  = "http://localhost:9000/public/images/width-350px.png"
  val landscapePNG = "http://localhost:9000/public/images/width-510px.png"
  val fbAppId = "375687459147542"
  val fbAppSecret = "d38e551a2eb9b7c97fbb3bfb2896d426"

  def settings() = {
    request.method match {
      case "POST" => {
        println("POST data")
        println(params.allSimple())
      }
      case _ => {
        val form = AccountSettingsForm(
          fullname = Field(name = "fullname", values = List("Will Chan")),
          username = Field(name = "username", values = List("willchan")),
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
        views.frontend.html.account_settings(form)
      }
    }
  }

  def gallery(user: String = "userdude", count: Int =  1, role: String = "other", pending: Int = 0) = {
    val completed = makeEgraphs(user)
    val pending = makePendingEgraphs(user)

    val egraphs = pending ::: completed

    views.frontend.html.account_gallery(user, egraphs, roles(role))
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
        productPublicName = Option("Jimmy Fallon"),
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
        productPublicName = Option("Built To Spill"),
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
        downloadUrl=Option("egr.aphs/" + user + "1"),
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        fbAppId =fbAppId,
        redirectURI = "http://staging.egraphs.com/" + user,
        orderId = 3,
        orientation = "landscape",
        productUrl="egr.aphs/" + user +"/1",
        productTitle = "Man or Velociraptor?",
        productPublicName = Option("Chris Bosh"),
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = landscapePNG
      ),
      FulfilledEgraphViewModel(
        downloadUrl=Option("egr.aphs/" + user + "2"),
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        fbAppId = fbAppId,
        redirectURI = "http://staging.egraphs.com/" + user,
        orderId = 4,
        orientation = "portrait",
        productUrl="egr.aphs/" + user +"/2",
        productTitle = "King James",
        productPublicName = None,
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = portraitPNG
      ),
      FulfilledEgraphViewModel(
        downloadUrl=Option("egr.aphs/" + user + "2"),
        publicStatus = "public",
        signedTimestamp = "Nov 12th 2012 @ 4:30 PM",
        fbAppId = fbAppId,
        redirectURI = "http://staging.egraphs.com/" + user,
        orderId = 5,
        orientation = "landscape",
        productUrl="egr.aphs/" + user +"/2",
        productTitle = "A cool bro",
        productPublicName = Option("Dwyane Wade"),
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst.",
        thumbnailUrl = landscapePNG
      )
    )
  }
}