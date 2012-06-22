package services.http

import services.cache.ApplicationCache
import com.google.inject.Inject
import scala.collection.immutable.Map
import collection.{mutable, TraversableLike}
import collection.mutable.ListBuffer
import play.mvc.Scope.Session
import services.logging.Logging

/**
 * Cache for a single user session. The cache is keyed by the session ID cookie provided
 * by Play.
 *
 * In addition to the specific usage here you can use it however you would any other
 * traversable: e.g. you can filter, map, etc over the tuples.
 *
 * Usage:
 * {{{
 *   class MyClass @Inject() (sessionFactory: () => ServerSession) {
 *
 *     private def session = {
 *       sessionFactory()
 *     }
 *
 *     // Add customerId to the session
 *     session.setting("customerId" -> 1).save()
 *
 *     // Set celebrityId and adminId
 *     session.setting("celebrityId" -> 1, "adminId" -> 2).save()
 *
 *     // Get adminId
 *     session[String]("adminId")  // should be Some(2)
 *
 *     // Delete adminId
 *     session.removing("adminId").save()
 *
 *     // Delete celebrity and customer id
 *     session.removing("celebrityId", "customerId").save()
 *
 *     // Clear out the entire session
 *     session.emptied.save()
 *
 *   }
 * }}}
 *
 * @param providedData Optional data of this current instance. If this is None then
 *     the instance will reach out to the Application cache to grab it.
 * @param services services needed for the ServerSession to operate correctly.
 */
class ServerSession private[http] (
  providedData: Option[Map[String, Any]],
  services: ServerSessionServices
) extends Traversable[(String, Any)] with TraversableLike[(String, Any), ServerSession]
{
  /**
   * Stores the map represented by this object into the [[services.cache.ApplicationCache]]
   *
   * @return the instance that was stored
   */
  def save(): ServerSession = {
    if (data.isEmpty) {
      appCache.delete(cacheKey)
    } else {
      appCache.set(cacheKey, data, Some("30d"))
    }

    this
  }

  /**
   * Empties all tuples from the cache
   *
   * @return the emptied cache
   */
  def emptied: ServerSession = {
    this.withData(Map.empty[String, Any])
  }

  /**
   * Gets a tuple Option from the cache. See class docs for usage.
   *
   * @param key the key to get from the cache
   * @tparam T the type of tuple.
   * @return Some(the value) or None if the key wasn't found
   */
  def apply[T : Manifest](key: String): Option[T] = {
    data.get(key).map(value => value.asInstanceOf[T])
  }

  /**
   * Returns a copy of the cache with the new tuples set into it. This
   * will override previous settings.
   *
   * See class documentation for usage info.
   *
   * @param keyValues the new tuples
   * @return
   */
  def setting (keyValues: (String, Any) *): ServerSession = {
    this.withData(data ++ keyValues)
  }

  /**
   * Returns a copy of the cache with the tuple corresponding to the
   * provided keys removed from it.
   *
   * @param keys keys for the tuples to remove
   */
  def removing (keys: String*): ServerSession = {
    this.withData(data -- keys)
  }

  //
  // Traversable and TraversableLike members
  //
  override def foreach[U](f: ((String, Any)) => U) {
    data.foreach(f)
  }

  override protected[this] def newBuilder: mutable.Builder[(String, Any), ServerSession] = {
    new ListBuffer[(String, Any)]().mapResult(tuples => this.withData(tuples.toMap))
  }

  //
  // Private members
  //
  private def withData(newData: Map[String, Any]) = {
    new ServerSession(providedData=Some(newData), services=services)
  }

  private lazy val data: Map[String, Any] = {
    // Return, in order of precedence, (1) the provided data (2) data from the cache,
    // (3) an empty Map[String, String]
    providedData.getOrElse {
      appCache.get[Map[String, Any]](cacheKey).getOrElse {
        Map.empty[String, Any]
      }
    }
  }

  private def session: Session = {
    services.sessionFactory()
  }

  private[http] def cacheKey: String = {
    "session_" + session.getId
  }

  private def appCache: ApplicationCache = {
    services.appCache
  }
}


case class ServerSessionServices @Inject() (
  sessionFactory: () => play.mvc.Scope.Session,
  appCache: ApplicationCache
)


object ServerSession extends Logging {
  // Uncomment this and finish the canBuildFrom implementation if you find that you need some of the
  // methods provided by having a custom CanBuildFrom implicit for the type (e.g. ++). See the
  // Traversable and TraversableLike scaladocs to see which methods benefit from having an
  // implicit CanBuildFrom. We've avoided it because, being static, it's impossible
  // to get the "right" Guice injector into the scope, so it would always have to be the default.
  //
  // If that confused the hell out of you like it did me, check out the following Gist:
  // https://gist.github.com/1136259
  //
  /*def cacheFact:ServerSessionFactory = null

  override protected[this] def newBuilder: mutable.Builder[(String, Any), ServerSession] = {
    new ListBuffer[(String, Any)]().mapResult(tuples => new ServerSession(null, null, withData(tuples.toMap)))
  }


  implicit def canBuildFrom = {
    new CanBuildFrom[Traversable[(String,Any)], (String,Any), ServerSession] {
      def apply() = newBuilder
      def apply(from: Traversable[(String,Int)]) = newBuilder
    }
  }*/
}


/**
 * Default factory that yields new session caches.
 *
 * @param cacheServices services needed for new instances of ServerSession
 */
private[http] class ServerSessionFactory @Inject() (cacheServices:ServerSessionServices) extends (() => ServerSession) {
  def apply(): ServerSession = {
    new ServerSession(providedData=None, services=cacheServices)
  }
}
