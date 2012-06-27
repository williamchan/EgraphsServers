package controllers

import play.mvc.Controller

/**
 * Hook into sublime text build system
 */
object Build extends Controller {

  /** 
   * Simply returns that the build is successful, which is always true
   * if the controller actually gets called.
   */
  def index = {
    "Build successful! " + new java.util.Date
  }
}