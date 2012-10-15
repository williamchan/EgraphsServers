package controllers.website.admin

import play.api.mvc.Controller
import play.mvc.results.RenderBinary
import services.AppConfig
import services.db.Schema
import services.http.{AdminRequestFilters, ControllerMethod}
import services.report._

private[controllers] trait GetReportsAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  private lazy val schema = AppConfig.instance[Schema]

  def getReportsAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {admin =>

      val action = params.get("action")
      action match {

        case null => {
          views.html.Application.admin.admin_reports()
        }

        case "email-list-report" => {
          val report = new EmailListReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "inventory-batch-report" => {
          val report = new InventoryBatchReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "order-report" => {
          val report = new OrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "physical-print-report" => {
          val report = new PrintOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "monthly-celebrity-order-report" => {
          val report = new MonthlyCelebrityOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }
        case "monthly-celebrity-print-order-report" => {
          val report = new MonthlyCelebrityPrintOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "sales-tax-report" => {
          val report = new SalesTaxReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "board-report" => {
          val report = new BoardReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case _ => "Not a valid action"
      }
    }
  }
}
