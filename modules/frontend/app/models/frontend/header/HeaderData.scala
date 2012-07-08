package models.frontend.header

import services.mvc.ImplicitHeaderAndFooterData

/**
 * Data necessary for rendering the photo
 * @param loggedInStatus the logged in status of the viewer. This is used to differentially
 *     render the "sign in" portion of the header.
 * @param insideAnEgraphLink link to the inside-an-egraph page
 * @param egraphsTwitterLink link to our twitter page
 * @param egraphsFacebookLink link to our facebook page
 */
case class HeaderData(
  loggedInStatus: Either[HeaderNotLoggedIn, HeaderLoggedIn] = Left(HeaderNotLoggedIn("/")),
  insideAnEgraphLink: String = "/inside-an-egraph",
  egraphsTwitterLink: String = ImplicitHeaderAndFooterData.twitterLink,
  egraphsFacebookLink: String = ImplicitHeaderAndFooterData.facebookLink
)