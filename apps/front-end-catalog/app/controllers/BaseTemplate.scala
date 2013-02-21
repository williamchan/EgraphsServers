package controllers

import play.api._
import play.api.mvc._
import play.api.templates.Html
import models.frontend.header.{HeaderLoggedIn, HeaderNotLoggedIn, HeaderData}
import models.frontend.footer.FooterData
import helpers.DefaultHeaderAndFooterData
import helpers.DefaultImplicitTemplateParameters
import helpers.DefaultAuthenticityToken

/**
 * Permutations of the base template
 */
object BaseTemplate extends Controller with DefaultAuthenticityToken
{
  def notLoggedIn = Action {
    implicit val headerData = DefaultHeaderAndFooterData.defaultHeaderData
    implicit val footerData = DefaultHeaderAndFooterData.defaultFooterData

    Ok(views.html.frontend.base_template(
      "Title",
      headJs = Html("var thisShouldHaveBeenInjectedInTheHead = 1;"),
      body = Html("<div>This is the body</div>"),
      jsMain = "pages/base-template"
    ))
  }

  def loggedIn = Action {
    val loggedInStatus = HeaderLoggedIn(
      name = "Herp Derpson",
      username = "herp4life",
      profileUrl = "/users/herpderpson1",
      accountSettingsUrl = "/users/herpderpson1/settings",
      galleryUrl = "/users/herpderpson1/gallery",
      "/logout"
    )

    implicit val headerData = DefaultHeaderAndFooterData.defaultHeaderData.copy(
      loggedInStatus=Right(loggedInStatus)
    )
    implicit val footerData = DefaultHeaderAndFooterData.defaultFooterData

    Ok(views.html.frontend.base_template(
      "Title",
      headJs = Html("var thisShouldHaveBeenInjectedInTheHead = 1;"),
      body = Html("<div>This is the body</div>"),
      jsMain = "pages/base-template"
    ))
  }

}
