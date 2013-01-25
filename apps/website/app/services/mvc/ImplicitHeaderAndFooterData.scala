package services.mvc

import models.frontend.header.{DeploymentInformation, HeaderNotLoggedIn, HeaderLoggedIn, HeaderData}
import models.frontend.footer.FooterData
import services.http.EgraphsSession.Conversions._
import play.api.mvc.Session
import models.{Customer, CustomerStore}
import controllers.routes.WebsiteControllers.{getCustomerGalleryByUsername, getAccountSettings}
import play.api.mvc.RequestHeader
import controllers.WebsiteControllers
import services.mail.BulkMailList

/**
 * Provides implicit data necessary to render the header and footer of the website's
 * base template. See front-end module `app/views/base_template.scala.html`
 */
trait ImplicitHeaderAndFooterData {
  protected def customerStore: CustomerStore
  protected def bulkMailList: BulkMailList

  implicit def siteHeaderData(implicit request: RequestHeader): HeaderData = {
    HeaderData(loggedInStatus=getHeaderLoggedInStatus(request.session),
      deploymentInformation = Option(DeploymentInformation(System.getProperty("deploymentTime")))
    )
  }

  implicit def siteFooterData: FooterData = {
    FooterData(
      aboutUsLink = controllers.routes.WebsiteControllers.getAbout.url,
      faqLink = controllers.routes.WebsiteControllers.getFAQ.url,
      termsOfUseLink = controllers.routes.WebsiteControllers.getTerms.url,
      privacyPolicyLink = controllers.routes.WebsiteControllers.getPrivacy.url,
      mailUrl = controllers.routes.WebsiteControllers.postSubscribeEmail.url
    )
  }

  //
  // Private methods
  //
  private def getHeaderLoggedInStatus(session: Session): Either[HeaderNotLoggedIn, HeaderLoggedIn] = {
    val headerLoggedInOption = getCustomerOption(session).map { customer =>
      val url = getCustomerGalleryByUsername(customer.username).url

      HeaderLoggedIn(
        name=customer.name,
        profileUrl="",
        accountSettingsUrl=getAccountSettings.url,
        galleryUrl=url,
        logoutUrl="/logout"
      )
    }

    headerLoggedInOption.toRight(HeaderNotLoggedIn("/login"))
  }

  private def getCustomerOption(session: Session): Option[Customer] = {
    for (
      customerId <- session.customerId;
      customer <- customerStore.findById(customerId)
    ) yield {
      customer
    }
  }

  private def userUrl(user: String, lastPart: String):String = {
    "users/" + user + "/" + lastPart
  }

}
