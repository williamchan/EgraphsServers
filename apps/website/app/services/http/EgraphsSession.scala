package services.http

import com.google.inject.Inject
import services.Utils
import play.api.mvc.Session

case class EgraphsSession(session: Session) {
  import EgraphsSession.Key
  
  // We have to manually implement session id =(
  def id: Option[String] = {
    session.get(EgraphsSession.SESSION_ID_KEY)
  }

  def adminId: Option[Long] = {
    getLong(Key.AdminId.name)
  }
  
  def withAdminId(id: Long): Session = {
    session + (Key.AdminId.name -> id.toString)
  }
  
  def customerId: Option[Long] = {
    getLong(Key.CustomerId.name)
  }
  
  def withCustomerId(id: Long): Session = {
    session + (Key.CustomerId.name -> id.toString)
  }

  def getLong(key: String): Option[Long] = {
    try {
      session.get(key).map(value => value.toLong)
    } catch {
      case _: NumberFormatException => None
    }
  }
}

object EgraphsSession {
  val SESSION_ID_KEY = "___ID"

  /**
   * Acceptable objects to use as keys on the EgraphsSession.
   */
  sealed abstract class Key { def name: String }

  object Key extends Utils.Enum {
    abstract class EnumVal(val name: String) extends Key with Value

    val AdminId = new EnumVal("admin") {}
    val CustomerId = new EnumVal("customer") {}
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

