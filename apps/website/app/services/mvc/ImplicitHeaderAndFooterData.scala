package services.mvc

import models.frontend.header.{HeaderLoggedInStatus, HeaderNotLoggedIn, HeaderLoggedIn, HeaderData}
import models.frontend.footer.FooterData
import play.mvc.Scope.Session
import controllers.WebsiteControllers
import services.http.EgraphsSession
import models.{Customer, CustomerStore}

/**
 * Provides implicit data necessary to render the header and footer of the website's
 * base template. See front-end module `app/views/base_template.scala.html`
 */
trait ImplicitHeaderAndFooterData {
  protected def egraphsSessionFactory: () => EgraphsSession
  protected def customerStore: CustomerStore

  implicit def siteHeaderData: HeaderData = {
    HeaderData(loggedInStatus=getHeaderLoggedInStatus)
  }

  implicit def siteFooterData: FooterData = {
    // TODO(erem): After integrating static pages then replace these hard links with
    // relative ones.
    FooterData()
  }

  //
  // Private methods
  //
  private def getHeaderLoggedInStatus: Either[HeaderNotLoggedIn, HeaderLoggedIn] = {
    val headerLoggedInOption = getCustomerOption.map { customer =>
      def customerUrl(lastPart: String) = "users/" + customer + "/" + lastPart

      HeaderLoggedIn(
        name=customer.name,
        profileUrl=customerUrl("profile"),
        accountSettingsUrl=customerUrl("settings"),
        galleryUrl=customerUrl("/gallery"),
        logoutUrl="/logout"
      )
    }

    headerLoggedInOption.toRight(HeaderNotLoggedIn("/login"))
  }

  private def egraphsSession: EgraphsSession = {
    egraphsSessionFactory()
  }

  private def getCustomerOption: Option[Customer] = {
    for (customerId <- egraphsSession.getLong(EgraphsSession.Key.CustomerId);
         customer <- customerStore.findById(customerId)
    ) yield {
      customer
    }
  }

  private def userUrl(user: String, lastPart: String):String = {
    "users/" + user + "/" + lastPart
  }

}

object ImplicitHeaderAndFooterData {
  val twitterLink = "http://www.twitter.com/egraphs"
  val facebookLink = "http://www.facebook.com/egraphs"
}