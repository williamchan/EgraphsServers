package services.http

import egraphs.playutils.Enum
import org.joda.time.DateTimeConstants
import play.api.mvc.Session
import play.api.mvc.Call
import services.request.PostCelebrityRequestHelper

case class EgraphsSession(session: Session) {
  import EgraphsSession.Key
  
  // We have to manually implement session id =(
  def id: Option[String] = {
    session.get(EgraphsSession.SESSION_ID_KEY)
  }

  def adminId: Option[Long] = {
    getLong(Key.AdminId.name)
  }

  def isUsernameChanged: Option[Boolean] = {
    getBoolean(Key.UsernameChanged.name)
  }

  def withAdminId(id: Long): Session = {
    session + (Key.AdminId.name -> id.toString)
  }

  def removeAdminId(): Session = {
    session - Key.AdminId.name
  }

  def withMlbpaAccess(): Session = {
    session + (Key.MlbpaAccess.name -> "1")
  }

  def removeMlbpaAccess(): Session = {
    session - Key.MlbpaAccess.name
  }

  def customerId: Option[Long] = {
    getLong(Key.CustomerId.name)
  }
  
  def withCustomerId(id: Long): Session = {
    session + (Key.CustomerId.name -> id.toString)
  }

  def removeCustomerId(): Session = {
    session - Key.CustomerId.name
  }

  def requestedStar: Option[String] = {
    session.get(Key.RequestedStar.name)
  }

  def withRequestedStar(requestedStar: String): Session = {
    session + (Key.RequestedStar.name -> requestedStar)
  }

  def removeRequestedStar: Session = {
    session - Key.RequestedStar.name
  }

  def withUsernameChanged: Session = {
    session + (Key.UsernameChanged.name -> true.toString)
  }

  def getLong(key: String): Option[Long] = {
    try {
      session.get(key).map(value => value.toLong)
    } catch {
      case _: NumberFormatException => None
    }
  }

  def getBoolean(key: String): Option[Boolean] = {
    session.get(key).map(value => java.lang.Boolean.valueOf(value))
  }

  // Convenience method for request a star feature that figures out post-login redirect
  def requestedStarRedirectOrCall(customerId: Long, otherCall: Call): Call = {
    val maybeRequestedStar = requestedStar
    maybeRequestedStar match {
      case None => otherCall
      case Some(requestedStar) => {
        PostCelebrityRequestHelper.completeRequestStar(requestedStar, customerId)
        controllers.routes.WebsiteControllers.getMarketplaceResultPage(vertical = "")
      }
    }
  }
}

object EgraphsSession {
  val SESSION_ID_KEY = "___ID"

  val COOKIE_MAX_AGE = 3 * 52 * DateTimeConstants.SECONDS_PER_WEEK

  /**
   * Acceptable objects to use as keys on the EgraphsSession.
   */
  sealed abstract class Key { def name: String }

  object Key extends Enum {
    abstract class EnumVal(val name: String) extends Key with Value

    val AdminId = new EnumVal("admin") {}
    val CustomerId = new EnumVal("customer") {}
    val MlbpaAccess = new EnumVal("mlbpa") {}
    val UsernameChanged = new EnumVal("username_changed") {}
    val HasSignedUp = new EnumVal("has_signed_up") {}
    val RequestedStar = new EnumVal("requested_star") {}
    val SignupModalDisplayedRecently = new EnumVal("signup_modal_displayed_recently") {}
  }

  object Conversions {
    implicit def sessionToEgraphsSession(session: Session): EgraphsSession = {
      EgraphsSession(session)
    }

    implicit def egraphsSessionToSession(egraphsSession: EgraphsSession): Session = {
      egraphsSession.session
    }
  }
}

