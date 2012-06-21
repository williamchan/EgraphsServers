package controllers

import play.mvc.Controller
import models.frontend.egraphs._
import java.text.SimpleDateFormat
import models.frontend.egraphs.Egraph


object Account extends Controller {

  val roles = Map ("other" -> OtherGalleryControl,
                        "admin" -> AdminGalleryControl,
                        "owner" -> OwnerGalleryControl)
  val thumbnails = Map("landscape" -> "http://placehold.it/500x400",
                        "portrait" -> "http://placehold.it/400x560")
  def settings() = {
    views.frontend.html.account_settings()
  }

  def gallery(user: String = "userdude", count: Int =  1, role: String = "other", pending: Int = 0) = {
    val completed = makeEgraphs(user)
    val pending = makePendingEgraphs(user)

    val egraphs = completed ::: pending

    views.frontend.html.account_gallery(user, egraphs.slice(0, count), roles(role))
  }

  private def makePendingEgraphs(user: String) : List[Egraph] = {
    val date = "November 19, 2012"
    List(
      Egraph( productUrl="egr.aphs/" + user +"/1",
        downloadUrl="egr.aphs/" + user + "1",
        orderUrl = "#",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
          "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
          " volutpat at.",
        productTitle = "Jimmy Fallon: Telling Jokes",
        signedTimestamp = date,
        orientation = "portrait",
        thumbnailUrl = thumbnails("portrait"),
        publishedStatus = "published",
        orderDetails = Option(OrderDetails(
                        orderNumber = 1, price = "$50.00",
                        orderDate = date,
                        statusText = "In progress",
                        shippingMethod = "UPS",
                        UPSNumber = "45Z343YHYU3343322J")),
        orderStatus = "pending"
      ),
      Egraph(productUrl="egr.aphs/" + user +"/1",
        downloadUrl="egr.aphs/" + user + "1",
        orderUrl = "#",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
          "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
          " volutpat at.",
        productTitle = "Velociraptor: Dinosaur or NBA Player?",
        signedTimestamp = date,
        orientation = "landscape",
        thumbnailUrl = thumbnails("landscape"),
        publishedStatus = "published",
        orderDetails = Option(OrderDetails(
          orderNumber = 2, price = "$120.00",
          orderDate = date,
          statusText = "In progress",
          shippingMethod = "UPS",
          UPSNumber = "45Z343YHYU3343322J")),
        orderStatus = "pending")
    )
  }
  private def makeEgraphs(user: String): List[Egraph]  = {
    val date = "November 19, 2021"
    List(
      Egraph( productUrl="egr.aphs/" + user +"/1",
              downloadUrl="egr.aphs/" + user + "1",
              orderUrl = "#",
              productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
                " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
                "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
                "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
                " volutpat at.",
              productTitle = "Chris Bosh: Man or Velociraptor?",
              signedTimestamp = date,
              orientation = "landscape",
              thumbnailUrl = thumbnails("landscape"),
              publishedStatus = "published",
              orderStatus = "finished"
      ),
      Egraph( productUrl="egr.aphs/" + user + "/2",
        downloadUrl="egr.aphs/" + user + "2",
        orderUrl = "#",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. Donec miami heat. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. miami heat miami heat massa, vitae venenatis mauris" +
          " volutpat at.",
        productTitle = "Lebron James: The King",
        signedTimestamp = date,
        orientation = "portrait",
        thumbnailUrl = thumbnails("portrait"),
        publishedStatus = "published",
        orderStatus = "finished"
      ),
      Egraph( productUrl="egr.aphs/" + user + "/3",
        downloadUrl="egr.aphs/" + user + "3",
        orderUrl = "#",
        productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
          "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
          " quam. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
          "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
          " volutpat at.",
        productTitle = "Dwyane Wade: Also a Cool Guy",
        signedTimestamp = date,
        orientation = "landscape",
        thumbnailUrl = thumbnails("landscape"),
        publishedStatus = "published",
        orderStatus = "finished"
      )
    )
  }
}