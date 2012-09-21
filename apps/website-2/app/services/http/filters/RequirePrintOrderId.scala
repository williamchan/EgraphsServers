package services.http.filters

import com.google.inject.Inject

import models.PrintOrderStore
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.NotFound
import services.http.PrintOrderRequest


// TODO: PLAY20 migration. Test and comment this summbitch.
class RequirePrintOrderId @Inject() (printOrderStore: PrintOrderStore) {
  def apply[A](printOrderId: Long, parser: BodyParser[A] = parse.anyContent)(operation: PrintOrderRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { request =>     
      val maybeResult = for (
        printOrder <- printOrderStore.findById(printOrderId)
      ) yield {
        operation(PrintOrderRequest(printOrder, request))
      }
      
      // TODO: PLAY20 migration actually redirect this to the reverse-route of GetLoginPrintOrderEndpoint
      //   instead of returning  a forbidden.
      maybeResult.getOrElse(noPrintOrderIdResult)
    }
  } 

  def inRequest[A](parser: BodyParser[A] = parse.anyContent)(operation: PrintOrderRequest[A] => Result)
  : Action[A] = 
  {
    Action(parser) { implicit request =>
      Form(single("printOrderId" -> longNumber)).bindFromRequest.fold(
        errors => noPrintOrderIdResult,
        printOrderId => this.apply(printOrderId, parser)(operation)(request)
      )
    }
  }
  
  //
  // Private members
  //
  private val noPrintOrderIdResult = NotFound("Valid PrintOrder ID was required but not provided")
}
