package controllers

import play.api._
import play.api.mvc._
import play.api.templates.Html
import models.frontend.header.{HeaderLoggedIn, HeaderNotLoggedIn, HeaderData}
import models.frontend.footer.FooterData

/**
 * Permutations of the base template
 */
object BaseTemplate extends Controller {
  def notLoggedIn = Action {
    implicit val headerData = defaultHeaderData
    implicit val footerData = defaultFooterData

    Ok(views.html.frontend.base_template(
      "Title",
      headJs=new Html("var thisShouldHaveBeenInjectedInTheHead = 1;"),
      body=new Html("<div>This is the body</div>"),
      jsMain="pages/base-template"
    ))
  }

  def loggedIn = Action {
    val loggedInStatus = HeaderLoggedIn(
      name="Herp Derpson",
      profileUrl="/users/herpderpson1",
      accountSettingsUrl="/users/herpderpson1/settings",
      galleryUrl="/users/herpderpson1/gallery",
      "/logout"
    )

    implicit val headerData = defaultHeaderData.copy(loggedInStatus=Right(loggedInStatus))
    implicit val footerData = defaultFooterData

    Ok(views.html.frontend.base_template(
      "Title",
      headJs=new Html("var thisShouldHaveBeenInjectedInTheHead = 1;"),
      body=new Html("<div>This is the body</div>"),
      jsMain="pages/base-template"
    ))
  }

  val defaultFooterData = {
    FooterData(
      aboutUsLink="about-us",
      faqLink="faq-link",
      termsOfUseLink="terms-of-use",
      privacyPolicyLink="privacy-policy",
      careersPolicyLink="careers",
      egraphsFacebookLink="http://www.facebook.com/egraphs",
      egraphsTwitterLink="http://www.twitter.com/egraphs"
    )
  }

  val defaultHeaderData = {
    HeaderData(
      loggedInStatus=Left(HeaderNotLoggedIn("/login-link")),
      insideAnEgraphLink="inside-an-egraph",
      egraphsFacebookLink="http://www.facebook.com/egraphs",
      egraphsTwitterLink="http://www.twitter.com/egraphs"
    )
  }
}

/** Provides a controller with a default implicit def for HeaderData */
trait DefaultHeaderAndFooterData {
  implicit def headerData:HeaderData = {
    BaseTemplate.defaultHeaderData
  }

  implicit def footerData:FooterData = {
    BaseTemplate.defaultFooterData
  }
}