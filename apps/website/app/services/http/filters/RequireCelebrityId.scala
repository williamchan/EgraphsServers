package services.http.filters

import com.google.inject.Inject
import models.CelebrityStore
import play.api.data.Form
import play.api.data.Forms.longNumber
import play.api.data.Forms.single
import play.api.mvc.Action
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.mvc.Result
import play.api.mvc.Results.Forbidden
import models.{ Account, Celebrity }
import play.api.libs.iteratee.{ Done, Input }
import play.api.mvc.RequestHeader
import play.api.mvc.Request

/**
 * Filter for requiring a celebrity by the id that is in the celebrity store.
 */
class RequireCelebrityId @Inject() (celebStore: CelebrityStore) extends Filter[Long, Celebrity] with RequestFilter[Long, Celebrity] {

  override def filter(celebId: Long): Either[Result, Celebrity] = {
    celebStore.findById(celebId).toRight(left = noCelebIdResult)
  }

  override val form: Form[Long] = Form(
    single(
      "celebrityId" -> longNumber)
      verifying ("Invalid celebrityId", {
        case celebrityId => celebrityId > 0
      }: Long => Boolean))

  override protected def badRequest(formWithErrors: Form[Long]): Result = noCelebIdResult

  def inAccount[A](account: Account, parser: BodyParser[A] = parse.anyContent)(actionFactory: Celebrity => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      val maybeResult = account.celebrityId.map { celebId =>
        val action = this(celebId, parser)(actionFactory)

        action(request)
      }

      maybeResult.getOrElse(noCelebIdResult)
    }
  }

  def filterInAccount(account: Account): Either[Result, Celebrity] = {
    for (
      celebId <- account.celebrityId.toRight(left = noCelebIdResult).right;
      celeb <- this.filter(celebId).right
    ) yield {
      celeb
    }
  }

  private val noCelebIdResult = Forbidden("Valid celebrity account credentials were required but not provided")
}