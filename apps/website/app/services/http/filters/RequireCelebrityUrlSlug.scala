package services.http.filters

import com.google.inject.Inject

import models.Celebrity
import models.CelebrityStore
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

/**
 * NOTE: These are the scaladocs for the old CelebrityAccountRequestFilters.requireCelebrityUrlSlug
 * which has been replaced.
 *
 * Prefer using celebrityUrlSlugOrNotFound
 *
 * Filters out requests that didn't provide a valid `celebrityUrlSlug` parameter.
 *
 * Calls the `continue` callback parameter with the corresponding [[models.Celebrity]] if the filter
 * passed.
 *
 * @param continue function to call if the request passed the filter
 * @param request the request whose params should be checked by the filter
 *
 * @return the return value of continue if the filter passed, otherwise `403-Forbidden`
 */
class RequireCelebrityUrlSlug @Inject() (celebrityStore: CelebrityStore) extends Filter[String, Celebrity] {
  override def filter(urlSlug: String): Either[Result, Celebrity] = {
    celebrityStore.findByUrlSlug(urlSlug) match {
      case None => Left(noCelebritySlugResult)
      case Some(celebrity) => Right(celebrity)
    }
  }

  private val noCelebritySlugResult = NotFound("No celebrity exists with this url")
}