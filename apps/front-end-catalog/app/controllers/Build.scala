package controllers

import play.api._
import play.api.mvc._

/**
 * Hook into sublime text build system
 */
object Build extends Controller {

  /** 
   * Simply returns that the build is successful, which is always true
   * if the controller actually gets called.
   */
  def index = Action {
    Ok("Build successful! " + new java.util.Date)
  }
}