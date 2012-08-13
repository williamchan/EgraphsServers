package controllers.website.admin

import play.mvc.Controller
import models._
import models.enums.EgraphState
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.Schema
import services.http.{AdminRequestFilters, ControllerMethod}

/**
 * These actions can be executed by any admin.
 */
private[controllers] trait GetToolsAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod

  protected def adminFilters: AdminRequestFilters

  private lazy val schema = AppConfig.instance[Schema]
  private lazy val enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]

  def getToolsAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {
      admin =>
        views.Application.admin.html.admin_tools()
    }
  }
}
