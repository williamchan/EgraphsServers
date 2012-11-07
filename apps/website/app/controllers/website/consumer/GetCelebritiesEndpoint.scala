package controllers.website.consumer

private[Consumer] trait GetCelebritiesEndpoint 
extends ImplicitHeaderAndFooterData 
{ this: Controller =>
  
  protected def controllerMethod: ControllerMethod
  protected def httpFilters
  
  def getMarketplace = controllerMethod.withForm() { implicit authToken =>
    // a bunch of stuff
    
  }
  
//  def getMarketplaceVertical(verticalUrlSlug: String) = controllerMethod.withForm() {
//    implicit authToken =>
//      // a bunch of stuff
//  }
  
//  def getCelebritySearch = controllerMethod.withForm() { implicit authToken => 
//    
//  }

  def getCelebritiesByFilter  = controllerMethod.withForm(){ implicit authToken => 
    Action { implicit request =>
      val form = Form(mapping())   
    }
    
  }

}