package controllers.website.consumer

import play.mvc.Controller
import services.http.{SafePlayParams, EgraphsSession, POSTControllerMethod}
import services.mvc.ImplicitHeaderAndFooterData
import services.db.{TransactionReadCommitted, TransactionSerializable, DBSession}
import services.mail.BulkMail
import services.http.forms.purchase.FormReaders
import services.http.forms.Form
import Form.Conversions._
import play.mvc.results.RenderJson
import sjson.json.Serializer


private[controllers] trait PostBulkEmailController extends ImplicitHeaderAndFooterData {
  this: Controller =>
  import SafePlayParams.Conversions._

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
      errors =>
        new RenderJson(Serializer.SJSON.toJSON(
          Map("errors" ->
            Serializer.SJSON.toJSON(
              errors.map( error =>
                error.description).toSeq
            )
          )
        )),
      validForm => {
        bulkMail.subscribeNew(validForm.listId, validForm.email)
        new RenderJson(Serializer.SJSON.toJSON(Map("subscribed" -> true)))
      }
    )
  }
}
