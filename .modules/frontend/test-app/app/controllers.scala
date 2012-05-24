//
// Controllers for testing the views you write in .modules/frontend/app/views
// Use alongside .modules/frontend/app/test-app/conf/routes to render your pages.
//
package controllers

import play._
import play.mvc._
import models._ // This gives us ExampleFrontendProject

object Application extends Controller {
  import views.frontend
  
  def index = {
    // Render the landing page
    frontend.example.html.uses_base_template()
  }

  /**
   * Shows a very simple template. Located at /Application/simple_example
   */
  def simple_example = {
    frontend.example.html.simple("herp derp")
  }

  /**
   * Shows how to pass a list of model objects into a template. /Application/model_example
   */
  def model_example = {
    val products = Seq(
      ExampleFrontendProduct(id="1", name="My First Product", price="50"),
      ExampleFrontendProduct(id="2", name="My Second Product", price="100")
    )
    
    frontend.example.html.uses_model(products)
  }

  /**
   * Shows how to re-use code by passing templates into other templates.
   * In this case base_template_example.
   */
  def code_reuse_example = {
    frontend.example.html.uses_base_template()
  }
}
