package services.http.filters

import com.google.inject.Inject

import models.enums.PublishedStatus
import models.{Celebrity, CelebrityAccesskey}
import play.api.mvc.Results.NotFound
import play.api.mvc.Result

class RequireCelebrityPublishedAccess @Inject() extends Filter[(Celebrity, String), Celebrity] {
  private def productNotFoundResult(celebName: String, productUrlSlug: String): Result = {
    NotFound(celebName + " doesn't have any product with url " + productUrlSlug)
  }

  override def filter(celebrityAndAccesskey: ((Celebrity, String))): Either[Result, Celebrity] = {
    val celebrity = celebrityAndAccesskey._1
    val accesskey = celebrityAndAccesskey._2
    if (celebrity.publishedStatus == PublishedStatus.Published
      || CelebrityAccesskey.matchesAccesskey(accesskey, celebrity.id)) {
      Right(celebrity)
    } else {
      Left(productNotFoundResult(celebrity.publicName, celebrity.urlSlug))
    }
  }
}
