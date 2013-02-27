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

  private val displayFrequencyInSeconds = 2 * DateTimeConstants.SECONDS_PER_WEEK

  private def displayedRecently(cookies: Cookies): Boolean = {
    val maybeLastSignupModalDisplay = cookies.get(SignupModalDisplayedRecently.name).map(cookie => java.lang.Boolean.valueOf(cookie.value).booleanValue)
    
    maybeLastSignupModalDisplay.getOrElse(false)
  }

  def shouldDisplay[A](implicit request: Request[A]): Boolean = {
    def loggedIn = request.session.customerId.isDefined
    def signedUp = request.cookies.get(HasSignedUp.name).map(cookie => java.lang.Boolean.valueOf(cookie.value).booleanValue).getOrElse(false)

    !loggedIn && !signedUp && !displayedRecently(request.cookies)
  }

  def apply[A](parser: BodyParser[A] = parse.anyContent)(actionFactory: Boolean => Action[A]): Action[A] = {
    Action(parser) { implicit request =>
      val displayModal = shouldDisplay(request)
      val result = actionFactory(displayModal)(request)
      if (displayModal) {
        result.withCookies(Cookie(SignupModalDisplayedRecently.name, true.toString, maxAge = Some(displayFrequencyInSeconds), secure = false))
      } else {
        result
      }
    }
  }
}