package controllers.website.admin

import play.mvc.Controller
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
          views.Application.admin.html.admin_reports()
        }

        // Email lists
        case "email-list-report" => {
          val report = new EmailListReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        // Inventory Batches
        case "inventory-batch-report" => {
          val report = new InventoryBatchReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        // Order Report
        case "order-report" => {
          val report = new OrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        // Physical Print Report
        case "physical-print-report" => {
          val report = new PrintOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        // Monthly Celebrity Payment Reports
        case "monthly-celebrity-order-report" => {
          val report = new MonthlyCelebrityOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }
        case "monthly-celebrity-print-order-report" => {
          val report = new MonthlyCelebrityPrintOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case _ => "Not a valid action"
      }
    }
  }
}