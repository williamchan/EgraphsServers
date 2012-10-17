package controllers.website.admin

import play.api.mvc.{Action, Controller}
import services.AppConfig
import services.db.Schema
import services.http.ControllerMethod
import services.http.filters.HttpFilters
import services.report._
import play.api.data._
import play.api.data.Forms._

private[controllers] trait GetReportsAdminEndpoint {
  this: Controller =>

  // 
  // Injected services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def schema: Schema
  
  //
  // Controllers
  //
  def getReportsAdmin = controllerMethod() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val action: String = Form("action" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
	    action match {
          case "" => {
            Ok(views.html.Application.admin.admin_reports())
          }
	
	      case "email-list-report" => {
	        val report = new EmailListReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "inventory-batch-report" => {
	        val report = new InventoryBatchReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "order-report" => {
	        val report = new OrderReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "physical-print-report" => {
	        val report = new PrintOrderReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "monthly-celebrity-order-report" => {
	        val report = new MonthlyCelebrityOrderReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	      case "monthly-celebrity-print-order-report" => {
	        val report = new MonthlyCelebrityPrintOrderReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "sales-tax-report" => {
	        val report = new SalesTaxReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case "board-report" => {
	        val report = new BoardReport(schema).report()
	        Ok.sendFile(report, false)
	      }
	
	      case _ => BadRequest("Not a valid action")
      	}
      }
    }
  }
}
