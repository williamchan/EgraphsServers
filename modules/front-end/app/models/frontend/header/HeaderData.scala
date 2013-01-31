package models.frontend.header

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
  sessionId: String = "1",
  insideAnEgraphLink: String = "/inside-an-egraph",
  egraphsTwitterLink: String = "http://www.twitter.com/egraphs",
  egraphsFacebookLink: String = "http://www.facebook.com/egraphs",
  ourStarsLink: String = "/stars",
  giftCertificateLink: Option[String] = None,
  deploymentInformation: Option[DeploymentInformation] = None
)
