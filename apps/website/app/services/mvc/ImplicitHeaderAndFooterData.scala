package services.mvc

import models.frontend.header.{HeaderNotLoggedIn, HeaderLoggedIn, HeaderData}
import models.frontend.footer.FooterData
import services.http.EgraphsSession
import models.{Customer, CustomerStore}
import controllers.WebsiteControllers
import services.mail.BulkMail

/**
 * Provides implicit data necessary to render the header and footer of the website's
 * base template. See front-end module `app/views/base_template.scala.html`
 */
trait ImplicitHeaderAndFooterData {
  protected def egraphsSessionFactory: () => EgraphsSession
  protected def customerStore: CustomerStore
  protected def bulkMail: BulkMail

  implicit def siteHeaderData: HeaderData = {
    HeaderData(loggedInStatus=getHeaderLoggedInStatus)
  }

  implicit def siteFooterData: FooterData = {
    FooterData(
      aboutUsLink = WebsiteControllers.reverse(WebsiteControllers.getAbout).url,
      faqLink = WebsiteControllers.reverse(WebsiteControllers.getFAQ).url,
      termsOfUseLink = WebsiteControllers.reverse(WebsiteControllers.getTerms).url,
      privacyPolicyLink = WebsiteControllers.reverse(WebsiteControllers.getPrivacy).url,
      careersPolicyLink = WebsiteControllers.reverse(WebsiteControllers.getCareers).url,
      newsletterListId = bulkMail.newsletterListId
    )
  }

  //
  // Private methods
  //
  private def getHeaderLoggedInStatus: Either[HeaderNotLoggedIn, HeaderLoggedIn] = {
    val headerLoggedInOption = getCustomerOption.map { customer =>
      val url = WebsiteControllers.reverse(WebsiteControllers.getCustomerGalleryByUsername(customer.username)).url

      HeaderLoggedIn(
        name=customer.name,
        profileUrl="",
        accountSettingsUrl=WebsiteControllers.reverse(WebsiteControllers.getAccountSettings).url,
        galleryUrl=url,
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
