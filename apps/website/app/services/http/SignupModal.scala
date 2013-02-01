package services.http

import java.util.Date
import com.google.inject.Inject
import org.joda.time.DateTimeConstants
import play.api.mvc.Action
import play.api.mvc.Request
import play.api.mvc.Result
import play.api.mvc.BodyParser
import play.api.mvc.BodyParsers.parse
import play.api.templates.Html
import services.http.EgraphsSession.Conversions._
import services.http.EgraphsSession.Key._
import egraphs.playutils.RichResult.resultToRichResult

class SignupModal @Inject() {

  private val displayFrequencyInMillis = 2 * DateTimeConstants.MILLIS_PER_WEEK

  private def notDisplayedRecently(date: Date): Boolean = {
    val thresholdDate = new Date(System.currentTimeMillis - displayFrequencyInMillis)
    date.before(thresholdDate)
  }

  def shouldDisplay[A](implicit request: Request[A]): Boolean = {
    if (request.session.hasSignedUp) {
      false
    } else {
      val maybeLastSignupModalDisplay = request.session.lastSignupModalDisplay
      def loggedIn = request.session.customerId.isDefined

      val maybeNotDisplayedRecently = maybeLastSignupModalDisplay.map { date =>
        notDisplayedRecently(date)
      }

      maybeNotDisplayedRecently.getOrElse(!loggedIn)
    }
  }

  def apply[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Boolean => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      val displayModal = shouldDisplay(request)
      val result = actionFactory(displayModal)(request)
      if (displayModal) {
        result.addingToSession((LastSignupModalDisplay.name -> System.currentTimeMillis.toString))
      } else {
        result
      }
    }
  }
}