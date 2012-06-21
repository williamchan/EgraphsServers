package controllers

import play.mvc.Controller
import models.frontend.egraphs.{OwnerGalleryControl, OtherGalleryControl, Egraph, AdminGalleryControl}
import java.text.SimpleDateFormat


object Account extends Controller {

  val roleLookup = Map ("other" -> OtherGalleryControl,
                        "admin" -> AdminGalleryControl,
                        "owner" -> OwnerGalleryControl)
  def settings() = {
    views.frontend.html.account_settings()
  }

  def gallery(user: String = "userdude", count: Int =  1, role: String = "other") = {
    val egraphs = List(makeCompletedEgraph(user), makeCompletedEgraph(user, "portrait"))

    views.frontend.html.account_gallery(user, egraphs, roleLookup(role))
  }

  private def makeCompletedEgraph(user: String, orientation: String = "landscape"): Egraph  = {
    val date = new SimpleDateFormat("MM-dd-yyyy HH:mm").parse("11-19-2012 14:05")
    val thumbnailUrl = orientation match {
      case "landscape" => "http://placehold.it/500x400"
      case "portrait" => "http://placehold.it/400x560"
      case _ => "http://placehold.it/500x400"
    }

    Egraph( productUrl="egr.aphs/" + user,
            downloadUrl="egr.aphs/" + user + "1",
            orderUrl = "#",
            productDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
              "Praesent blandit mollis dui, sed venenatis neque sodales nec. Aliquam ut semper" +
              " quam. In hac habitasse platea dictumst. Etiam at lectus at nisi blandit lobort" +
              "is. Donec viverra rhoncus iaculis. In a nibh tellus. Phasellus dignissim egesta" +
              "s erat nec vestibulum. Proin blandit pellentesque massa, vitae venenatis mauris" +
              " volutpat at.",
            productTitle = "David Ortiz: Homerun",
            signedTimestamp = date,
            orientation = orientation,
            thumbnailUrl = thumbnailUrl,
            publishedStatus = "published",
            orderStatus = "complete"
    )
  }
}