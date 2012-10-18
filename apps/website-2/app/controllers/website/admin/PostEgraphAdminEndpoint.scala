package controllers.website.admin

import models._
import enums.EgraphState
import controllers.WebsiteControllers
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import services.http.SafePlayParams.Conversions._
import services.logging.Logging
import play.api.mvc.Action
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraints._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid

trait PostEgraphAdminEndpoint { this: Controller =>

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def egraphStore: EgraphStore

  def postEgraphAdmin(egraphId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
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
		      egraph.order.sendEgraphSignedMail(request)
		      Redirect(GetEgraphAdminEndpoint.url(egraphId))
		    }
		    case _ => Forbidden("Unsupported operation")
	      }
      	}
      }
    }
  }
}
