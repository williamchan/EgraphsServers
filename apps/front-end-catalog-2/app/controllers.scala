//
// Controllers for testing the views you write in .modules/frontend/app/views
// Use alongside apps/front-end-catalog/conf/routes to render your pages.
//
package controllers

import play.api._
import play.api.mvc._
import models.frontend.ExampleFrontendProduct
import java.lang.reflect.Method
import java.lang.Class
import play.api.templates.Html

object Application extends Controller {
  
  def index() = Action {
    // Render the landing page
    Ok(views.html.catalog())
  }

  /**
   * Shows a very simple template. Located at /Application/simple_example
   */
  def simple_example = Action {
    Ok(views.html.frontend.example.simple("herp derp"))
  }

  /**
   * Shows how to pass a list of model objects into a template. /Application/model_example
   */
  def model_example = Action {
    val products = Seq(
      ExampleFrontendProduct(id="1", name="My First Product", price="50"),
      ExampleFrontendProduct(id="2", name="My Second Product", price="100")
    )
    
    Ok(views.html.frontend.example.uses_model(products))
  }

  /**
   * Shows how to re-use code by passing templates into other templates.
   * In this case base_template_example.
   */
  def code_reuse_example = Action {
    Ok(views.html.frontend.example.uses_base_template())
  }
}

