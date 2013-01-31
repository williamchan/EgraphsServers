package views.frontend
import play.Play
import java.io.File
import controllers.Assets
import scala.annotation.tailrec
import models.frontend.egraphs.EgraphViewModel

/**
 * Front-end utilities that otherwise had no home.
 */
object Utils {

  /**
   *  Returns a string for an angular.js binding
   *
   **/
  def binding(model: String, id: String) : String = {
    "{{" + model + id + "}}"
  }

  def getFacebookShareLink(appId: String,
                           picUrl: String,
                           name: String,
                           caption: String,
                           description: String,
                           link: String): String = {

    "https://www.facebook.com/dialog/feed?" +
      "app_id=" + appId +
      "&redirect_uri=" + link +
      "&picture=" + picUrl +
      "&name=" + name +
      "&caption=" + caption +
      "&description=" + description +
      "&link=" + link
  }

  def getTwitterShareLink(link: String,
                          text: String): String = {
    "https://twitter.com/intent/tweet?" +
      "url=" + link +
      "&text=" + text
  }
}
