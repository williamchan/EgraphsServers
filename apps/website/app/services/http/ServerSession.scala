package services.http

import services.cache.{Cache, CacheFactory}
import com.google.inject.Inject
import scala.collection.immutable.Map
import collection.{mutable, TraversableLike}
import collection.mutable.ListBuffer
import play.api.mvc.Session
import services.logging.Logging
import services.http.EgraphsSession.Conversions._
import services.{Namespacing, AppConfig, Time}
import play.api.mvc.Session

/**
 * Cache for a single user session. The cache is keyed by the session ID cookie provided
 * by Play.
 *
 * In addition to the specific usage here you can use it however you would any other
 * traversable: e.g. you can filter, map, etc over the tuples.
 *
 * You can create namespaced copies of this session, which effectively creates "folders"
 * within the session that you can separately populate and blow away.
 *
 * Usage:
 * {{{
 *   class MyClass @Inject() (sessionFactory: ServerSessionFactory) {
 *     Action { request =>
 *       private def session = {
 *         sessionFactory(request.session)
 *       }
 *
 *       // Add customerId to the session
 *       session.setting("customerId" -> 1).save()
 *
 *       // Set celebrityId and adminId
 *       session.setting("celebrityId" -> 1, "adminId" -> 2).save()
 *
 *       // Get adminId
 *       session[String]("adminId")  // should be Some(2)
 *
 *       // Delete adminId
 *       session.removing("adminId").save()
 *
 *       // Delete celebrity and customer id
 *       session.removing("celebrityId", "customerId").save()
 *
 *       // Create a folder called "shopping-cart"
 *       val cart = session.namespaced("shopping-cart")
 *
 *       // Save the number of items into the cart.
 *       // The absolute key becomes "shopping-cart/numItems"
 *       cart.setting("numItems", 0).save()
 *
 *       // Clear the cart. This wouldn't delete anything outside of the cart.
 *       cart.emptied.save()
 *
 *       // Clear out the entire session
 *       session.emptied.save()
 *
 *     }
 *   }
 * }}}
 *
 * @param providedData Optional data of this current instance. If this is None then
 *     the instance will reach out to the Application cache to grab it.
 * @param namespace the current namespace of this server session instance, e.g. "shopping-cart".
 *     A namespace allows you to create a named "sub-folder" within the session.
 * @param services services needed for the ServerSession to operate correctly.
 */
class ServerSession private[http] (
  providedData: Option[Map[String, Any]],
  val namespace: String="",
  services: ServerSessionServices = AppConfig.instance[ServerSessionServices]
) extends Traversable[(String, Any)]
  with TraversableLike[(String, Any), ServerSession]
  with Namespacing
{
  import Time.IntsToSeconds._
  /**
   * Stores the map represented by this object into the [[services.cache.ApplicationCache]]
   *
   * @return the instance that was stored
   */
  def save(): ServerSession = {
    if (data.isEmpty) {
      appCache.delete(cacheKey)
    } else {
      appCache.set(cacheKey, data, 30.days)
    }

    this
  }

  /**
   * Create a new "folder" or "keyspace" inside of the session. That keyspace can be separately
   * added to, removed from, and cleared out without affecting parent keys.
   *
   * @param newNamespace name of the new space, e.g. "shopping-cart"
   * @return a ServerSession representing the new namespace.
   */
  def namespaced(newNamespace: String): ServerSession = {
    new ServerSession(providedData, applyNamespace(newNamespace), services)
  }

  def namespaces: Traversable[ServerSession] = {
    // Iterate through the keys in this namespace and choose unique set of ones that have subkeys
    val namespaces = namespacedData.foldLeft(Set.empty[String]) { (namespaces, nextKeyValue) =>
      val key = nextKeyValue._1

      val keyParts = key.replaceFirst(applyNamespace(to=""), "").split("/")

      println("Removing " + applyNamespace("") + " from " + key + " produced " + keyParts.toList)

      if (keyParts.size > 1) {
        namespaces + keyParts.head
      } else {
        namespaces
      }
    }

    // Create namespace sessions out of the uniques
    namespaces.map(ns => this.namespaced(ns))
  }

  /**
   * Empties all tuples from this namespace of the session
   *
   * @return the emptied session
   */
  def emptied: ServerSession = {
    this.withData(data -- namespacedData.keySet)
  }

  /**
   * Gets a tuple Option from the session. See class docs for usage.
   *
   * @param key the key to get from the session
   * @tparam T the type of tuple.
   * @return Some(the value) or None if the key wasn't found
   */
  def apply[T : Manifest](key: String): Option[T] = {
    data.get(applyNamespace(key)).map(value => value.asInstanceOf[T])
  }

  /** Alias for apply */
  def get[T : Manifest](key: String): Option[T] = {
    apply(key)
  }

  /**
   * Returns a copy of the session with the new tuples set into it. This
   * will override previous settings.
   *
   * See class documentation for usage info.
   *
   * @param keyValues the new tuples
   * @return
   */
  def setting (keyValues: (String, Any) *): ServerSession = {
    this.withData(data ++ applyNamespace(keyValues))
  }

  /**
   * Returns a copy of the session with the tuple corresponding to the
   * provided keys removed from it.
   *
   * @param keys keys for the tuples to remove
   */
  def removing (keys: String*): ServerSession = {
    this.withData(data -- keys.map(key => applyNamespace(key)))
  }

  //
  // Traversable and TraversableLike members
  //
  override def foreach[U](f: ((String, Any)) => U) {
    namespacedData.foreach(f)
  }

  override protected[this] def newBuilder: mutable.Builder[(String, Any), ServerSession] = {
    new ListBuffer[(String, Any)]().mapResult { tuples =>
      this.withData(applyNamespace(tuples).toMap)
    }
  }

  //
  // Private members
  //
  private def withData(newData: Map[String, Any]) = {
    new ServerSession(providedData=Some(newData), namespace=namespace, services=services)
  }

  private lazy val data: Map[String, Any] = {
    // Return, in order of precedence, (1) the provided data (2) data from the session,
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
    "session_" + session.id.getOrElse(
      throw new RuntimeException("Encountered request with no session ID")
    )
  }

  private def appCache: Cache = {
    services.cacheFactory.applicationCache
  }

  private def applyNamespace(to: Traversable[(String, Any)]): Traversable[(String, Any)] = {
    for ((key, value) <- to) yield (applyNamespace(key), value)
  }

  private def namespacedData: Map[String, Any] = {
    if (namespace == "") {
      data
    } else {
      data.filterKeys(key => key.startsWith(namespace))
    }
  }
}


case class ServerSessionServices @Inject() (
  sessionFactory: () => Session,
  cacheFactory: CacheFactory
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
 * @param sessionServices services needed for new instances of ServerSession
 */
class ServerSessionFactory @Inject() (cacheFactory: CacheFactory) extends {

  def apply(session: Session): ServerSession = {
    val services = ServerSessionServices(() => session, cacheFactory)
    
    new ServerSession(providedData=None, services=services)
  }

  /** Returns the server-session namespace associated with this user's shopping cart */
  def shoppingCart(session: Session): ServerSession = {
    this.apply(session).namespaced("cart")
  }

  /**
   * Returns the server-session namespace associated with the items in the user's shopping
   * cart that are associated with the given [[models.Celebrity]]'s storefront
   *
   * @param celebrityId id of the celebrity whose storefront items in the user's
   *   cart we should access.
   */
  def celebrityStorefrontCart(celebrityId: Long)(session: Session): ServerSession = {
    this.shoppingCart(session).namespaced("celeb-" + celebrityId)
  }
}
