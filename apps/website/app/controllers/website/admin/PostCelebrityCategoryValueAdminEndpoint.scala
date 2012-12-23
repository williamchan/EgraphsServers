package controllers.website.admin

import models._
import models.categories._
import play.api.mvc.Controller
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.mvc.Action
import services.mail.TransactionalMail

/**
 * This controller associates celebrities with CategoryValues i.e. "tags" the associated celeb.
 * It destructively dissociates a celeb from all of its categoryvalues and applys a new list from
 * the caller.
 */
trait PostCelebrityCategoryValueAdminEndpoint {
  this: Controller => 
  
  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def transactionalMail: TransactionalMail
  protected def celebrityStore: CelebrityStore
  protected def accountStore: AccountStore
  protected def categoryValueStore: CategoryValueStore
  
  case class CelebrityCategoryValueForm(
      categoryValueId: Long
  )

  def postCelebrityCategoryValueAdmin(celebrityId: Long) = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        
        val categoryValueIds = request.body.asFormUrlEncoded match {
          case Some(params) if(params.contains("categoryValueIds")) =>
            for(categoryValueId <- params("categoryValueIds")) yield {
              categoryValueId.toLong
            }

          case _ => List[Long]()
        }

        celebrityStore.findById(celebrityId) match {
            case Some(celebrity) =>
              celebrityStore.updateCategoryValues(celebrity = celebrity, categoryValueIds = categoryValueIds)
              Redirect(controllers.routes.WebsiteControllers.getCelebrityAdmin(celebrity.id).url, FOUND)

            case None => Redirect(controllers.routes.WebsiteControllers.getCelebritiesAdmin.url, SEE_OTHER)
          }
      }
    }  
  }
}