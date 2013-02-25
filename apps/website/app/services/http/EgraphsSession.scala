package services.http

import com.google.inject.Inject
import services.Utils
import play.api.mvc.Session
import play.api.mvc.Call
import egraphs.playutils.Enum
import java.util.Date

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

  def hasSignedUp: Boolean = {
    getBoolean(Key.HasSignedUp.name).getOrElse(false)
  }

  def lastSignupModalDisplay: Option[Date] = {
    getDate(Key.LastSignupModalDisplay.name)
  }

  def withAdminId(id: Long): Session = {
    session + (Key.AdminId.name -> id.toString)
  }

  def removedAdminId: Session = {
    session - Key.AdminId.name
  }

  def customerId: Option[Long] = {
    getLong(Key.CustomerId.name)
  }
  
  def withCustomerId(id: Long): Session = {
    session + (Key.CustomerId.name -> id.toString)
  }

  def removedCustomerId: Session = {
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

  def withHasSignedUp: Session = {
    session + (Key.HasSignedUp.name -> true.toString)
  }

  def withLastSignupModalDisplay: Session = {
    session + (Key.LastSignupModalDisplay.name -> System.currentTimeMillis.toString)
  }

  def getLong(key: String): Option[Long] = {
    try {
      session.get(key).map(value => value.toLong)
    } catch {
      case _: NumberFormatException => None
    }
  }

  def getDate(key: String): Option[Date] = {
    getLong(key).map(long => new Date(long))
  }

  def getBoolean(key: String): Option[Boolean] = {
    session.get(key).map(value => java.lang.Boolean.valueOf(value))
  }
}

object EgraphsSession {
  val SESSION_ID_KEY = "___ID"

  /**
   * Acceptable objects to use as keys on the EgraphsSession.
   */
  sealed abstract class Key { def name: String }

  object Key extends Enum {
    abstract class EnumVal(val name: String) extends Key with Value

    val AdminId = new EnumVal("admin") {}
    val CustomerId = new EnumVal("customer") {}
    val UsernameChanged = new EnumVal("username_changed") {}
    val HasSignedUp = new EnumVal("has_signed_up") {}
    val LastSignupModalDisplay = new EnumVal("last_signup_modal_display") {}
    val RequestedStar = new EnumVal("requested_star") {}
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

