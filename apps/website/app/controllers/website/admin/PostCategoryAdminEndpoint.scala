package controllers.website.admin

import models.categories._
import services.http.POSTControllerMethod
import services.http.filters.HttpFilters
import play.api.data._
import play.api.data.Forms._
import play.api.data.validation.Constraint
import play.api.data.validation.Valid
import play.api.data.validation.Invalid
import play.api.mvc._


trait PostCategoryAdminEndpoint {
  this: Controller =>    

  protected def postController: POSTControllerMethod
  protected def httpFilters: HttpFilters
  protected def categoryStore: CategoryStore
  
  case class PostCategoryForm(
     name: String,
     publicName: String
  )

  def postCategoryAdmin = postController() {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val categoryId = Form("categoryId" -> longNumber).bindFromRequest.fold(formWithErrors => 0L, validForm => validForm)
      	val isCreate = (categoryId == 0)
      	
      	val form = Form(mapping(
          "name" -> nonEmptyText(maxLength = 128),
          "publicName" -> nonEmptyText(maxLength = 128)
        )(PostCategoryForm.apply)(PostCategoryForm.unapply).verifying(isUniqueName(categoryId))
        )
        
        form.bindFromRequest.fold(
          formWithErrors => {
            val data = formWithErrors.data
            val errors = for (error <- formWithErrors.errors) yield {
              error.key + ": " + error.message
            }
            val url = if(isCreate) controllers.routes.WebsiteControllers.getCreateCategoryAdmin.url else controllers.routes.WebsiteControllers.getCategoryAdmin(categoryId).url
            Redirect(url, SEE_OTHER).flashing(
              ("errors" -> errors.mkString(", ")), 
  		        ("categoryId" -> categoryId.toString), 
  		        ("publicName" -> data.get("publicName").getOrElse("")), 
  		        ("name" -> data.get("name").getOrElse(""))
            )
          },
          validForm => {
            val tmp = if (isCreate) Category() else categoryStore.get(categoryId)
            val savedCategory = tmp.copy(
                publicName = validForm.publicName,
                name = validForm.name).save()
            Redirect(controllers.routes.WebsiteControllers.getCategoryAdmin(savedCategory.id).url, FOUND)
          }
        )
      }
  	}
  }
  
  private def isUniqueName(categoryId: Long): Constraint[PostCategoryForm] = {
    Constraint { form: PostCategoryForm => 
      categoryStore.findByName(form.name) match {
        case None => Valid
        case Some(category) if(category.id == categoryId) => Valid
        case _ => Invalid("Name must be unique")
      }
    }  
  }    
}