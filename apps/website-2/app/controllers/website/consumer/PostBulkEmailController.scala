package controllers.website.consumer

import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.mvc.ImplicitHeaderAndFooterData
import services.mail.BulkMail
import services.http.forms.purchase.FormReaders
import services.http.forms.Form
import Form.Conversions._
import play.api.libs.json.Json.toJson

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
  def postSubscribeEmail = postController(openDatabase = false) {
    /**
     * listSubscribe(string apikey, string id, string email_address, array merge_vars,
     * string email_type, bool double_optin, bool update_existing, bool replace_interests, bool send_welcome)
     */
    val formReadableParams = params.asFormReadable
    val subscriptionReader = formReaders.forEmailSubscriptionForm
    val subscriptionForm = subscriptionReader.instantiateAgainstReadable(formReadableParams)
    subscriptionForm.errorsOrValidatedForm.fold(
      errors => Ok(toJson(Map("error" -> errors.map( error => error.description).toSeq)))
      ,
      validForm => {
        bulkMail.subscribeNew(validForm.listId, validForm.email)
        Ok(toJson(Map("subscribed" -> true)))
      }
    )
  }
}
