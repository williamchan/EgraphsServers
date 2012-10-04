package services.http

import com.google.inject.Inject
import services.Utils
import play.api.mvc.Session

/**
 * Wraps the play client-side session and the server-side session, limiting the key-set
 * to Egraphs-specific keys.
 *
 * Usage:
 * {{{
 *   class MyClass @Inject()(egraphsSessionFactory: () => services.http.EgraphsSession) {
 *     // Import the acceptable keys
 *     import services.http.EgraphsSession.Key
 *
 *     // Get an instance of the session
 *     val session = egraphsSessionFactory()
 *
 *     // Put some values into it
 *     val sessionWithData = session.withLong(Key.AdminId -> 10)
 *                                  .withString(Key.CustomerId -> "5")
 *
 *     // At this point a new session DOESNT have the values, because we havent
 *     // saved.
 *     egraphSessionFactory.getLong(Key.AdminId) == None // true
 *
 *     // We save...
 *     sessionWithData.save()
 *
 *     // And now a new session has the values
 *     egraphSessionFactory().getLong(Key.AdminId) == 10 // true
 *
 *     // Let's remove admin id
 *     egraphSessionFactory().deleting(Key.AdminId).save()
 *
 *     // Lets clear all of the keys.
 *     session.cleared.save()
 *   }
 * }}}
 *
 * @param cookieDataToSave a map that keeps track of changes we should be applying to
 *     the stateful play Session cookie.
 * @param services services needed for the EgraphsSession to function.
 */
class EgraphsSession @Inject()(
  cookieDataToSave: Map[EgraphsSession.Key, Option[String]]=Map.empty,
  services:EgraphsSessionServices
 ) {

  /**
   * Retrieves a string option from the egraphs session.
   *
   * @param key the key to look up in the session
   * @return Some(theValue) if it was available. Otherwise None.
   */
  def apply(key: EgraphsSession.Key): Option[String] = {
    cookieDataToSave.get(key) match {
      // Found a value (or None) in the data to be committed when we save
      case Some(valueOption) =>
        valueOption

      // Nothing found in cookieDataToSave, maybe the session has it?
      case None =>
        Option(playSession.get(key.name))
    }
  }

  /**
   * See the `apply` method
   */
  def get(key: EgraphsSession.Key): Option[String] = {
    apply(key)
  }

  /**
   * Retrieves a Long option from the egraphs session.
   */
  def getLong(key: EgraphsSession.Key): Option[Long] = {
    try {
      this.get(key).map(string => string.toLong)
    } catch {
      case _: NumberFormatException => None
    }
  }

  /**
   * Sets a string into the session. Changes are not persisted until you save.
   */
  def withString(keyValue: (EgraphsSession.Key, String)): EgraphsSession = {
    val (key, value) = keyValue
    this.withCookieData(cookieDataToSave + (key -> Some(value)))
  }

  /**
   * Sets a Long into the session. Changes are not persisted until you save.
   */
  def withLong(keyValue: (EgraphsSession.Key, Long)): EgraphsSession = {
    val (key, value) = keyValue

    this.withString(key, value.toString)
  }

  /**
   * Removes a key value pair from the session. Changes are not persisted until
   * you save.
   */
  def deleting(key: EgraphsSession.Key): EgraphsSession = {
    this.withCookieData(cookieDataToSave + (key -> None))
  }

  /**
   * Clears all values from the session. Changes are not persisted until
   * you save.
   */
  def cleared: EgraphsSession = {
    this.withCookieData(cookieDataToSave -- EgraphsSession.Key.values)
  }

  /**
   * Saves any data added to or removed from the session since grabbing it from
   * the factory.
   */
  def save(): EgraphsSession = {
    val session = playSession

    // Iterate over the pairs and either put them in the session or remove them
    for ((key, valueOption) <- cookieDataToSave) {
      valueOption match {
        case Some(value) =>
          session.put(key.name, value)

        case None =>
          session.remove(key.name)
      }
    }

    // Return ourselves without any data to save because it's all been saved now!
    this.withCookieData(Map())
  }

  //
  // Private members
  //
  private def playSession:Session = {
    services.playSessionFactory()
  }

  private def withCookieData(newCookieData: Map[EgraphsSession.Key, Option[String]]) = {
    new EgraphsSession(newCookieData, services)
  }
}


object EgraphsSession {
  /**
   * Acceptable objects to use as keys on the EgraphsSession.
   */
  sealed abstract class Key { def name: String }

  object Key extends Utils.Enum {
    abstract class EnumVal(val name: String) extends Key with Value

    val AdminId = new EnumVal("admin") {}
    val CustomerId = new EnumVal("customer") {}
    val RedirectUponLogin = new EnumVal("redirectUponLogin") {}
  }
}

/** Services used by the EgraphsSession. */
private[http] case class EgraphsSessionServices @Inject()(
  playSessionFactory: () => Session
)

/** Provisions an instance of EgraphsSession. Favor injecting () => EgraphsSession */
private[http] class EgraphsSessionFactory @Inject()(services: EgraphsSessionServices)
extends (() => EgraphsSession)
{
  def apply(): EgraphsSession = {
    new EgraphsSession(Map(), services)
  }
}