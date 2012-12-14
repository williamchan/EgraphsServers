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

  def heOrShe(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "he"
      case (true, true) => "He"
      case (false, false) => "she"
      case (false, true) => "She"
    }
  }

  def himOrHer(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "him"
      case (true, true) => "Him"
      case (false, false) => "her"
      case (false, true) => "Her"
    }
  }

  def hisOrHer(isMale: Boolean, capitalize: Boolean = false): String = {
    (isMale, capitalize) match {
      case (true, false) => "his"
      case (true, true) => "His"
      case (false, false) => "her"
      case (false, true) => "Her"
    }
  }
  
  def egraphOrEgraphs(numberOfEgraphs: Int): String = {
    if (numberOfEgraphs == 1) "Egraph"
    else "Egraphs"
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
  
  // Should only display on MY gallery page those egraphs that either I purchased myself
  // or that are published (i.e. don't spoil the gift-giving surprise)
  def displayableEgraphs(egraphs: List[EgraphViewModel], galleryCustomerId: Long) = {
    egraphs.filter(egraph => !egraph.isGift || (egraph.isGift && !egraph.isPending))
  }
  
  def areAllPendingGifts(egraphs: List[EgraphViewModel]) = {
    egraphs.forall(egraph => egraph.isPending && egraph.isGift)
  }
}
