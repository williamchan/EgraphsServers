package controllers.website.admin

import models._
import enums.EgraphState
import play.api.mvc.Results.Redirect
import play.api.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}

trait PostEgraphAdminEndpoint { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore

  def postEgraphAdmin(egraphId: Long) = postController() {
    adminFilters.requireEgraph {(egraph, admin) =>
      val egraphStateParam = params.get("egraphState")
      EgraphState.apply(egraphStateParam) match {
        case None => Forbidden("Not a valid egraph state")
        case Some(EgraphState.ApprovedByAdmin) => {
          egraph.approve(admin).save()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case Some(EgraphState.RejectedByAdmin) => {
          egraph.reject(admin).save()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case Some(EgraphState.Published) => {
          egraph.publish(admin).save()
          egraph.order.sendEgraphSignedMail()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
