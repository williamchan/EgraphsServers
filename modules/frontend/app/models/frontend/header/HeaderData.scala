package models.frontend.header

case class HeaderData(
  loggedInStatus: Either[HeaderNotLoggedIn, HeaderLoggedIn],
  insideAnEgraphLink: String,
  egraphsTwitterLink: String,
  egraphsFacebookLink: String  
)