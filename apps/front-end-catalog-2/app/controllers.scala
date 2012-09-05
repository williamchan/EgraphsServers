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

//  def route(controllerName: String, actionName: String) = Action {
//    try {
//      val controller = Class.forName("controllers." + controllerName)
//      val m = controller.getDeclaredMethods()
//      for (i <- 0 until m.length) {
//          println(m(i).toString())
//      }
//      val action = controller.getMethod(actionName)
//      println(action)
//    } catch {
//      case e: Exception => println(e)
//    }
//
//    Ok("You want to do: " + controllerName + " " + actionName)
//  }
//
  
  //#GET     /:controller/:action                    controllers.Application.route2(controller: String, action: String, user: Option[String], publicName: Option[String], casualName: Option[String], count: Option[Int], num: Option[Int], crumbIndex: Option[Int], focus: Option[Int])
//  def route2(controllerName: String, actionName: String,
//             user: Option[String],
//             publicName: Option[String],
//             casualName: Option[String],
//             count: Option[Int],
//             num: Option[Int],
//             crumbIndex: Option[Int],
//             focus: Option[Int]) = Action {
//    var action: Method = null
//    var parameters:List[Any] = List[Any]()
//    val controller = Class.forName("controllers." + controllerName)
//    try {
//      if(user.isDefined) {
//        parameters = user.get :: parameters 
//      }
//      if(count.isDefined) {
//        parameters = count.get :: parameters
//      }
//
//      val m = controller.getDeclaredMethods()
//      for (i <- 0 until m.length) {
//          println(m(i).toString())
//      }
//
//      def classToType(clazz: Class[_]): Class[_] = {
//        clazz.getName() match {
//          case "java.lang.Integer" => Integer.TYPE 
//          case _ => clazz
//        }
//      }
//      
//      val parameterTypes: Array[java.lang.Class[_]] = parameters.map(param => classToType(param.getClass())).toArray
//      action = controller.getMethod(actionName, parameterTypes: _*)
//      println(action)
//    } catch {
//      case e: Exception => println(e)
//    }
//
//    //TODO: remove this and debug line
//    if(action == null) {
//      Ok("You want to do: " + controllerName + " " + actionName + " num = " + num + " METHOD = " + action)
//    } else {
//      val args: Array[Any] = parameters.toArray
////      val controllerObj = controller.newInstance()
////            println("\nDoing this\n" + action + " " + parameters + "  controller = " + controllerObj)
//      val html = action.invoke(null, args)
//      Ok(html.asInstanceOf[Html])
//    }
//  }

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
