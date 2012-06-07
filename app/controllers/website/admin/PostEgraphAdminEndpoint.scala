package controllers.website.admin

import models._
import play.mvc.results.Redirect
import play.mvc.Controller
import services.http.{POSTControllerMethod, AdminRequestFilters}

trait PostEgraphAdminEndpoint { this: Controller =>
  protected def postController: POSTControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def egraphStore: EgraphStore

  def postEgraphAdmin(egraphId: Long) = postController() {
    adminFilters.requireEgraph {(egraph, admin) =>
      val egraphStateParam = params.get("egraphState")
      Egraph.EgraphState.all.get(egraphStateParam) match {
        case None => Forbidden("Not a valid egraph state")
        case Some(Egraph.EgraphState.ApprovedByAdmin) => {
          egraph.approve(admin).save()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case Some(Egraph.EgraphState.RejectedByAdmin) => {
          egraph.reject(admin).save()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case Some(Egraph.EgraphState.Published) => {
          egraph.publish(admin).save()
          egraph.order.sendEgraphSignedMail()
          new Redirect(GetEgraphAdminEndpoint.url(egraphId).url)
        }
        case _ => Forbidden("Unsupported operation")
      }
    }
  }
}
