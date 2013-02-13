package controllers.website.admin

import models._
import enums.EgraphState
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import services.email.ViewEgraphEmail

trait PostEgraphAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore

  def postEgraphAdmin(egraphId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      httpFilters.requireEgraphId(egraphId) { egraph =>
	    Action { implicit request =>
	      val egraphStateParam = Form("egraphState" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
		  EgraphState.apply(egraphStateParam) match {
		    case None => Forbidden("Not a valid egraph state")
		    case Some(EgraphState.ApprovedByAdmin) => {
		      egraph.approve(admin).save()
		      Redirect(GetEgraphAdminEndpoint.url(egraphId))
		    }
		    case Some(EgraphState.RejectedByAdmin) => {
		      egraph.reject(admin).save()
		      Redirect(GetEgraphAdminEndpoint.url(egraphId))
		    }
		    case Some(EgraphState.Published) => {
		      egraph.publish(admin).save()
		      ViewEgraphEmail(order = egraph.order).send() // send view egraph email
		      Redirect(GetEgraphAdminEndpoint.url(egraphId))
		    }
		    case _ => Forbidden("Unsupported operation")
	      }
      	}
      }
    }
  }
}
