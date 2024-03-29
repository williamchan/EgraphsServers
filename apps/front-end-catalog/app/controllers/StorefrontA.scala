package controllers

import egraphs.playutils.{MaleGrammar, FemaleGrammar}
import play.api.data._
import play.api.data.Forms._
import play.api.mvc.{Action, Controller}
import helpers.DefaultImplicitTemplateParameters
import models.frontend.storefront_a.{PersonalizeProduct, PersonalizeStar}
import org.joda.money.{CurrencyUnit, Money}

object StorefrontA extends Controller with DefaultImplicitTemplateParameters {
  def personalize = Action { request =>
    Ok(views.html.frontend.storefronts.a.personalize(
      star = star("Sergio Romo"),
      checkoutUrl = "/checkout",
      maxDesiredTextChars = 60,
      maxMessageToCelebChars = 100,
      testcase = Some("default")
    ))
  }

  def personalizeSoldOut = Action { request =>
    Ok(views.html.frontend.storefronts.a.personalize(
      star = star("Sergio Romo").copy(products=Nil, isSoldOut = true),
      checkoutUrl = "/checkout",
      maxDesiredTextChars = 60,
      maxMessageToCelebChars = 100,
      testcase = Some("default")
    ))
  }

  def checkout(testcase: Option[String]) = Action { request =>
    val request = Form
    Ok(views.html.frontend.storefronts.a.checkout(
      personalizeUrl = "/personalize",
      testcase = testcase
    ))
  }

  def star(starName: String) = {
    val products = makeProducts(starName, 3)
    PersonalizeStar(
      id = 1L,
      name = starName,
      products = products,
      grammar = FemaleGrammar,
      mastheadUrl = "https://d3kp0rxeqzwisk.cloudfront.net/celebrity/172/landing_20121119003405102/master.jpg",
      isSoldOut = products.isEmpty
    )
  }

  def makeProducts(star: String, n: Int=3): Seq[PersonalizeProduct] = {
    for ( i <- 1 to n) yield {
      PersonalizeProduct(
        id = i,
        title = star + " " + i,
        description = "The number " + i + " product of the distinguished grade \"A\" " + star,
        price = Money.of(CurrencyUnit.USD, i * 10),
        selected = (i == 2),
        smallThumbUrl = "https://d3kp0rxeqzwisk.cloudfront.net/product/416/20120823100121825/w340.jpg",
        largeThumbUrl = "https://d3kp0rxeqzwisk.cloudfront.net/product/416/20120823100121825/w575.jpg"
      )
    }
  }
}
