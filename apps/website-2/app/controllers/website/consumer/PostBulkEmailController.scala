package controllers.website.consumer

import play.api.mvc.Controller
import services.http.{POSTControllerMethod, WithoutDBConnection}
import services.mvc.ImplicitHeaderAndFooterData
import services.mail.BulkMail
import services.http.forms.purchase.FormReaders
import services.http.forms.Form
import Form.Conversions._
import play.api.libs.json.Json.toJson
import play.api.mvc.Action

private[controllers] trait PostBulkEmailController extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def postController: POSTControllerMethod
  protected def bulkMail: BulkMail
  protected def formReaders: FormReaders

  /**
   * Subscribe an email address to our bulkmail provider
   *
   * @return
   */
  def postSubscribeEmail = postController(dbSettings = WithoutDBConnection) {
    Action { implicit request =>
      /*
       * listSubscribe(string apikey, string id, string email_address, array merge_vars,
       * string email_type, bool double_optin, bool update_existing, bool replace_interests, bool send_welcome)
       */
      val formReadableRequest = request.asFormReadable
      val subscriptionReader = formReaders.forEmailSubscriptionForm
      val subscriptionForm = subscriptionReader.instantiateAgainstReadable(formReadableRequest)
      subscriptionForm.errorsOrValidatedForm.fold(
        errors => Ok(toJson(Map("error" -> errors.map( error => error.description).toSeq))),
        
        validForm => {
          bulkMail.subscribeNewAsync(validForm.listId, validForm.email)  //TODO: We aren't doing anything if this fails, maybe we should do something 
          //like store it somewhere that gets retried later since our bulk mailer service could be down..
          Ok(toJson(Map("subscribed" -> true)))
        }
      )
    }
  }
}
