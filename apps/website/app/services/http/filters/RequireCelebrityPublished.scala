package services.http.filters

import com.google.inject.Inject

import models.enums.PublishedStatus
import models.Celebrity
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

class RequireCelebrityPublished @Inject() extends Filter[Celebrity, Celebrity] {
  private def productNotFoundResult(celebName: String, productUrlSlug: String): Result = {
    NotFound(celebName + " doesn't have any product with url " + productUrlSlug)
  }

  override def filter(celebrity: Celebrity): Either[Result, Celebrity] = {
    if (celebrity.publishedStatus == PublishedStatus.Published) {
      Right(celebrity)
    } else {
      Left(productNotFoundResult(celebrity.publicName, celebrity.urlSlug))
    }
  }
}
