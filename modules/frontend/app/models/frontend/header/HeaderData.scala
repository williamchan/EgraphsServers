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
  loggedInStatus: Either[HeaderNotLoggedIn, HeaderLoggedIn],
  insideAnEgraphLink: String,
  egraphsTwitterLink: String,
  egraphsFacebookLink: String  
)