package controllers

import play.mvc.Controller
import models.Celebrity
import libs.Utils
import play.mvc.Router.ActionDefinition
import libs.Blobs.AccessPolicy


/**
 * Serves the website's Celebrity root page.
 */
object CelebrityController extends Controller
  with DBTransaction
  with RequiresCelebrityName
{
  /** Controller for a celebrity's home page. */
  def index = {
    val profilePhotoUrl = celebrity.profilePhoto.resizedWidth(200).getSaved(AccessPolicy.Public).url

    views.Application.html.celebrity(celebrity, profilePhotoUrl, celebrity.products())
  }

  /** Returns the home page for the provided Celebrity. */
  def url(celebrity: Celebrity): ActionDefinition = {
    Utils.lookupUrl(
      "CelebrityController.index",
      Map("celebrityUrlSlug" -> celebrity.urlSlug.get)
    )
  }
}