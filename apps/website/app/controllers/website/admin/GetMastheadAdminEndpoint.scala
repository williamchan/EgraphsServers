package controllers.website.admin

import services.mvc.ImplicitHeaderAndFooterData
<<<<<<< HEAD
import play.api.mvc.{Request, Action, Controller}
=======
import play.api.mvc.{Action, Controller}
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
import services.http.ControllerMethod
import models.{Masthead, MastheadStore}
import services.http.filters.HttpFilters


private[controllers] trait GetMastheadAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def mastheadStore: MastheadStore

<<<<<<< HEAD
  private val postMastheadUrl = controllers.routes.WebsiteControllers.postMastheadAdmin.url
=======
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
  def getMastheadAdmin(mastheadId: Long) = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action {implicit request =>
        val maybeMasthead = mastheadStore.findById(mastheadId)
        maybeMasthead match {
<<<<<<< HEAD
          case Some(masthead) => Ok(views.html.Application.admin.admin_masthead_detail(masthead, postMastheadUrl, errorFields))
=======
          case Some(masthead) => Ok(views.html.Application.admin.admin_masthead_detail(masthead))
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
          case None => NotFound("No masthead with this ID exists")
        }

      }
    }
  }

  def getCreateMastheadAdmin = controllerMethod.withForm() { implicit authToken =>
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action {implicit request =>
        Ok(views.html.Application.admin.admin_masthead_detail(Masthead(
          headline = "An Example headline",
          subtitle = Some("an example subtitle")
<<<<<<< HEAD
        ), postMastheadUrl, errorFields))
      }
    }
  }

  private def errorFields(implicit request: Request[play.api.mvc.AnyContent]) : Option[List[String]] = flash.get("errors").map(errString => errString.split(',').toList)

=======
        )))
      }
    }
  }
>>>>>>> 38b186c1ace05a975f964d59f7ef30d2e80da114
}
