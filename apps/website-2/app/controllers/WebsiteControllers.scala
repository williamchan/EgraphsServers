package controllers

import website._
import play.api.mvc.Action
import play.api.mvc.Controller
import play.api.mvc.Results.Redirect
import services.blobs.Blobs
import services.mail.{BulkMail, TransactionalMail}
import services.payment.Payment
import models._
import services.db.DBSession
import services.social.FacebookAppId
import services.http._
import services.http.filters._
import forms.{AccountRecoverFormFactory, AccountPasswordResetFormFactory, AccountSettingsFormFactory, CustomerLoginFormFactory}
import forms.purchase.{PurchaseFormChecksFactory, FormReaders, PurchaseFormFactory}
import services.mvc.celebrity.CatalogStarsQuery
import services.mvc.{OrderCompleteViewModelFactory, StorefrontBreadcrumbData}
import services.config.ConfigFileProxy

object WebsiteControllers extends Controller with AllWebsiteEndpoints
{
  import services.AppConfig.instance
  import services.AppConfig.annotatedInstance

  // Provide endpoint dependencies
  override protected val config = instance[ConfigFileProxy]
  override protected val facebookAppId = annotatedInstance[FacebookAppId, String]

  override protected def breadcrumbData = instance[StorefrontBreadcrumbData]
  override protected def customerLoginForms = instance[CustomerLoginFormFactory]
  override protected def accountSettingsForms = instance[AccountSettingsFormFactory]
  override protected def accountPasswordResetForms = instance[AccountPasswordResetFormFactory]
  override protected def accountRecoverForms = instance[AccountRecoverFormFactory]

  override protected def catalogStarsQuery: CatalogStarsQuery = instance[CatalogStarsQuery]
  override protected def orderCompleteViewModelFactory: OrderCompleteViewModelFactory = instance[OrderCompleteViewModelFactory]

  override protected def checkPurchaseField: PurchaseFormChecksFactory = instance[PurchaseFormChecksFactory]

  override protected val purchaseFormFactory = instance[PurchaseFormFactory]
  override protected val formReaders = instance[FormReaders]
  override protected val dbSession = instance[DBSession]
  override protected val controllerMethod = instance[ControllerMethod]
  override protected val postController = instance[POSTControllerMethod]
  override protected val httpFilters = instance[HttpFilters]

  override protected val blobs = instance[Blobs]
  override protected val transactionalMail = instance[TransactionalMail]
  override protected val bulkMail = instance[BulkMail]
  override protected val payment = instance[Payment]

  override protected val accountStore = instance[AccountStore]
  override protected val administratorStore = instance[AdministratorStore]
  override protected val celebrityStore = instance[CelebrityStore]
  override protected val customerStore = instance[CustomerStore]
  override protected val orderStore = instance[OrderStore]
  override protected val productStore = instance[ProductStore]
}
