package controllers

import play.mvc.Controller
import models.frontend.egraphs.{OwnerGalleryControl, OtherGalleryControl, Egraph, AdminGalleryControl}
import java.text.SimpleDateFormat


object Account extends Controller {

  val roles = Map ("other" -> OtherGalleryControl,
                        "admin" -> AdminGalleryControl,
                        "owner" -> OwnerGalleryControl)
  val thumbnails = Map("landscape" -> "http://placehold.it/500x400",
                        "portrait" -> "http://placehold.it/400x560")
  def settings() = {
    views.frontend.html.account_settings()
  }

  def gallery(user: String = "userdude", count: Int =  1, role: String = "other") = {
    val egraphs = makeEgraphs(user)

    views.frontend.html.account_gallery(user, egraphs.slice(0, count), roles(role))
  }

  private def makeEgraphs(user: String): List[Egraph]  = {
    val date = new SimpleDateFormat("MM-dd-yyyy HH:mm").parse("11-19-2012 14:05")
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