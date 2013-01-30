package views.frontend
import play.Play
import java.io.File
import controllers.Assets
import scala.annotation.tailrec
import models.frontend.egraphs.EgraphViewModel
import egraphs.playutils.Gender

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

  def heOrSheOrThey(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "he"
      case (Gender.Male, true) => "He"
      case (Gender.Female, false) => "she"
      case (Gender.Female, true) => "She"
      case (Gender.Neutral, false) => "they"
      case (Gender.Neutral, true) => "They"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def himOrHerOrThem(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "him"
      case (Gender.Male, true) => "Him"
      case (Gender.Female, false) => "her"
      case (Gender.Female, true) => "Her"
      case (Gender.Neutral, false) => "them"
      case (Gender.Neutral, true) => "Them"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def hisOrHerOrTheir(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "his"
      case (Gender.Male, true) => "His"
      case (Gender.Female, false) => "her"
      case (Gender.Female, true) => "Her"
      case (Gender.Neutral, false) => "their"
      case (Gender.Neutral, true) => "Their"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }
  
  def irregularToHave(gender: Gender.EnumVal, capitalize: Boolean = false): String = {
    (gender, capitalize) match {
      case (Gender.Male, false) => "has"
      case (Gender.Male, true) => "Has"
      case (Gender.Female, false) => "has"
      case (Gender.Female, true) => "Has"
      case (Gender.Neutral, false) => "have"
      case (Gender.Neutral, true) => "Have"
      case _ => throw new IllegalStateException("You are a very rare gender")
    }
  }

  def regularVerb(verb: String, gender: Gender.EnumVal): String = {
    gender match {
      case Gender.Male => verb + "s"
      case Gender.Female => verb + "s"
      case Gender.Neutral => verb
      case _ => throw new IllegalStateException("You are a very rare gender")
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
}
