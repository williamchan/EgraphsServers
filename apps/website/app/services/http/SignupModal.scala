package services.http

import java.util.Date
import com.google.inject.Inject
import org.joda.time.DateTimeConstants
import play.api.mvc._
import play.api.mvc.BodyParsers.parse
import play.api.templates.Html
import services.http.EgraphsSession.Conversions._
import services.http.EgraphsSession.Key._
import egraphs.playutils.ResultUtils.RichResult

class SignupModal @Inject() {

  private val displayFrequencyInMillis = 2 * DateTimeConstants.MILLIS_PER_WEEK

  private def notDisplayedRecently(session: Session): Boolean = {
    val maybeLastSignupModalDisplay = session.lastSignupModalDisplay
    maybeLastSignupModalDisplay.map { lastDisplayed => 
      val thresholdDate = new Date(System.currentTimeMillis - displayFrequencyInMillis)
      lastDisplayed.before(thresholdDate)
    }.getOrElse(true)
  }

  def shouldDisplay[A](implicit request: Request[A]): Boolean = {
    def loggedIn = request.session.customerId.isDefined
    def signedUp = request.session.hasSignedUp

    println(s"!hasSignedUp = ${!request.session.hasSignedUp}, !loggedIn = ${!loggedIn}, notDisplayedRecently = ${notDisplayedRecently(request.session)}")
    !loggedIn && !signedUp && notDisplayedRecently(request.session)
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