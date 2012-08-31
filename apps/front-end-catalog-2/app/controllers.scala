//
// Controllers for testing the views you write in .modules/frontend/app/views
// Use alongside apps/front-end-catalog/conf/routes to render your pages.
//
package controllers

import play.mvc._
import models.frontend.ExampleFrontendProduct

object Application extends Controller {
  
  def index = {
    // Render the landing page
    views.html.catalog()
  }

//  /**
//   * Shows a very simple template. Located at /Application/simple_example
//   */
//  def simple_example = {
//    views.frontend.example.html.simple("herp derp")
//  }
//
//  /**
//   * Shows how to pass a list of model objects into a template. /Application/model_example
//   */
//  def model_example = {
//    val products = Seq(
//      ExampleFrontendProduct(id="1", name="My First Product", price="50"),
//      ExampleFrontendProduct(id="2", name="My Second Product", price="100")
//    )
//    
//    views.frontend.example.html.uses_model(products)
//  }
//
//  /**
//   * Shows how to re-use code by passing templates into other templates.
//   * In this case base_template_example.
//   */
//  def code_reuse_example = {
//    views.frontend.example.html.uses_base_template()
//  }
}


/** Test controllers for the @safeForm tag. */
object SafeFormTestController extends Controller {
  import views.html

  def safeFormGET = {
    html.safeFormTest(postWasAuthenticated="Post not authenticated")
  }

  def safeFormPOST(authenticityToken: String) = {
//    if (session.getAuthenticityToken != authenticityToken) {
//      Forbidden
//    }
//    else {
      html.safeFormTest(postWasAuthenticated="Post authenticated")
//    }
  }
}
