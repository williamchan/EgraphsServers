package controllers.website.mlbpa

import controllers.PaginationInfoFactory
import models.{Celebrity, Egraph, EgraphQueryFilters, EgraphStore}
import models.enums.EgraphState
import models.frontend.email
import models.frontend.email.EmailViewModel
import models.frontend.egraph.MlbpaEgraphView
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.{Constraint, Invalid, Valid}
import play.api.data.validation.Constraints._
import play.api.mvc._
import services.http.{EgraphsSession, ControllerMethod, POSTControllerMethod}
import services.http.EgraphsSession.Conversions._
import services.http.filters.HttpFilters
import services.mvc.ImplicitHeaderAndFooterData
import services.blobs.AccessPolicy
import services.mail.TransactionalMail
import services.ConsumerApplication

private[controllers] trait MlbpaEndpoints extends ImplicitHeaderAndFooterData { this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def postController: POSTControllerMethod
  protected def egraphStore: EgraphStore
  protected def egraphQueryFilters: EgraphQueryFilters
  protected def transactionalMail: TransactionalMail
  protected def consumerApp: ConsumerApplication

  private val mlbpaUsername = "egraphsMlbpa"
  private val mlbpaPassword = "c8Eh6fyI5WS38Fs"

  def getMlbpaLogin = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      Ok(views.html.Application.mlbpa.mlbpa_login())
    }
  }

  def postMlbpaLogin = postController() {
    Action { implicit request =>
      val loginForm = Form(
        mapping(
          "username" -> text.verifying(nonEmpty),
          "password" -> text.verifying(nonEmpty)
        )(PostMlbpaLoginForm.apply)(PostMlbpaLoginForm.unapply).verifying(isValidMlbpaLogin)
      )
      loginForm.bindFromRequest.fold(
        formWithErrors => {
          Redirect(controllers.routes.WebsiteControllers.getMlbpaLogin).flashing("errors" -> formWithErrors.errors.head.message.toString)
        },
        validForm => {
          Redirect(controllers.routes.WebsiteControllers.getMlbpaEgraphs.url).withSession(request.session.withMlbpaAccess())
        }
      )
    }
  }

  def getMlbpaEgraphs = controllerMethod.withForm() { implicit authToken =>
    Action { implicit request =>
      if (hasMlbpaAccess(request.session)) {

        val page: Int = Form("page" -> number).bindFromRequest.fold(formWithErrors => 1, validForm => validForm)
        val query = egraphStore.getEgraphsAndResults(egraphQueryFilters.pendingMlbReview)
        val pagedQuery: (Iterable[(Egraph, Celebrity, _, _)], Int, Option[Int]) = services.Utils.pagedQuery(select = query, page = page)
        implicit val paginationInfo = PaginationInfoFactory.create(pagedQuery = pagedQuery, baseUrl = controllers.routes.WebsiteControllers.getMlbpaEgraphs.url)
        val mlbpaEgraphViews = for (result <- pagedQuery._1) yield {
          MlbpaEgraphView(
            celebrityName = result._2.publicName,
            egraphId = result._1.id,
            egraphMp4Url = result._1.getVideoAsset.getSavedUrl(AccessPolicy.Public)
          )
        }
        Ok(views.html.frontend.mlbpa.mlbpa_egraphs(egraphViews = mlbpaEgraphViews))

      } else {
        Redirect(controllers.routes.WebsiteControllers.getMlbpaLogin.url)
      }
    }
  }

  def postMlbpaEgraph(egraphId: Long) = postController() {
    httpFilters.requireEgraphId(egraphId) { egraph =>
      Action { implicit request =>
        if (hasMlbpaAccess(request.session)) {
          val egraphStateParam = Form("egraphState" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
          EgraphState.apply(egraphStateParam) match {
            case None => Forbidden("Not a valid egraph state")
            case Some(EgraphState.ApprovedByAdmin) => {
              egraph.withEgraphState(EgraphState.ApprovedByAdmin).save()
              Redirect(controllers.routes.WebsiteControllers.getMlbpaEgraphs.url)
            }
            case Some(EgraphState.RejectedByMlb) => {
              val rejectReason = Form("rejectReason" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
              sendEgraphRejectionNoticeEmail(egraphId, rejectReason)
              egraph.withEgraphState(EgraphState.RejectedByMlb).save()
              Redirect(controllers.routes.WebsiteControllers.getMlbpaEgraphs.url)
            }
            case _ => Forbidden("Unsupported operation")
          }
        } else {
          Forbidden("")
        }
      }
    }
  }

  private def hasMlbpaAccess(session: Session): Boolean = session.get(EgraphsSession.Key.MlbpaAccess.name).exists(_ == "1")

  private def sendEgraphRejectionNoticeEmail(egraphId: Long, rejectReason: String) {
    val egraphUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphAdmin(egraphId).url)
    val emailHeaders = email.EmailViewModel(subject = "MLB rejected " + egraphUrl + " - Reason: " + rejectReason, toAddresses = List(("mlbpa-rejection-notices@egraphs.com", None)))
    transactionalMail.send(mailStack = emailHeaders, templateContentParts = List(("mlbpa-rejection-notice", "")))
  }

  private case class PostMlbpaLoginForm(username: String, password: String)
  private def isValidMlbpaLogin: Constraint[PostMlbpaLoginForm] = {
    Constraint { form: PostMlbpaLoginForm =>
      if (form.username == mlbpaUsername && form.password == mlbpaPassword) Valid else Invalid("Invalid login")
    }
  }
}
