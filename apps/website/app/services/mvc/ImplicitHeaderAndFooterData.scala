package services.mvc

import models.frontend.header.{AnalyticsData, DeploymentInformation, HeaderNotLoggedIn, HeaderLoggedIn, HeaderData}
import models.frontend.footer.FooterData
import services.http.EgraphsSession.Conversions._
import play.api.mvc.Session
import models.{Customer, CustomerStore}
import controllers.routes.WebsiteControllers.{getCustomerGalleryByUsername, getAccountSettings}
import play.api.mvc.RequestHeader
import controllers.WebsiteControllers
import services.mail.BulkMailList
import services.config.ConfigFileProxy

/**
 * Provides implicit data necessary to render the header and footer of the website's
 * base template. See front-end module `app/views/base_template.scala.html`
 */
//TODO and analytics rename
trait ImplicitHeaderAndFooterData {
  protected def customerStore: CustomerStore
  protected def bulkMailList: BulkMailList
  protected def config: ConfigFileProxy

  implicit def siteHeaderData(implicit request: RequestHeader): HeaderData = {
    HeaderData(
      loggedInStatus = getHeaderLoggedInStatus(request.session),
      deploymentInformation = Option(DeploymentInformation(System.getProperty("deploymentTime"))),
      enableLogging=config.enableFrontendLogging,
      //TODO: move into it's own implicit object
      updateMixpanelAlias = shouldUpdateMixpanelAlias(request.session)
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

  //TODO probably should create a new combination of Header,Footer and Analytics object
  //so each template won't need to change every time this happens.
//  implicit def analyticsData(implicit request: RequestHeader): AnalyticsData = {
//    AnalyticsData(updateMixpanelAlias = shouldUpdateMixpanelAlias(request.session))
//  }

  //
  // Private methods
  //
  private def getHeaderLoggedInStatus(session: Session): Either[HeaderNotLoggedIn, HeaderLoggedIn] = {
    val headerLoggedInOption = getCustomerOption(session).map { customer =>
      val url = getCustomerGalleryByUsername(customer.username).url

      HeaderLoggedIn(
        name = customer.name,
        username = customer.username,
        profileUrl = "",
        accountSettingsUrl = getAccountSettings.url,
        galleryUrl = url,
        logoutUrl = controllers.routes.WebsiteControllers.getLogout.url
      )
    }

    headerLoggedInOption.toRight(HeaderNotLoggedIn(controllers.routes.WebsiteControllers.getLogin(None).url))
  }

  private def getCustomerOption(session: Session): Option[Customer] = {
    for (
      customerId <- session.customerId;
      customer <- customerStore.findById(customerId)
    ) yield {
      customer
    }
  }

  private def shouldUpdateMixpanelAlias(session: Session): Boolean = {
    session.isUsernameChanged.getOrElse(false)
  }
}
