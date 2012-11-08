package services.http.filters

import com.google.inject.Inject

import models.Celebrity
import models.CelebrityStore
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

/**
 * Rejects with 404 NotFound when the provided urlSlug did not correspond to a celebrity.
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