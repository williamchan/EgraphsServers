package services.http

import services.cache.ApplicationCache
import com.google.inject.Inject
import scala.collection.immutable.Map
import collection.{mutable, TraversableLike}
import collection.mutable.ListBuffer
import play.mvc.Scope.Session
import services.logging.Logging

class SessionCache private[http] (
  providedData: Option[Map[String, Any]],
  services: SessionCacheServices
) extends Traversable[(String, Any)] with TraversableLike[(String, Any), SessionCache]
{
  import SessionCache._

  //
  // Public members
  //
  def save(): SessionCache = {
    if (data.isEmpty) {
      appCache.delete(cacheKey)
    } else {
      appCache.set(cacheKey, data, Some("30d"))
    }

    this
  }

  def emptied: SessionCache = {
    this.withData(Map.empty[String, Any])
  }

  def apply[T : Manifest](key: String): Option[T] = {
    data.get(key).map(value => value.asInstanceOf[T])
  }

  def ++ (keyValues: Traversable[(String, Any)]): SessionCache = {
    this.withData(data ++ keyValues)
  }

  def + (keyValue: (String, Any)): SessionCache = {
    this.withData(data + keyValue)
  }

  def - (key: String): SessionCache = {
    this.withData(data - key)
  }

  //
  // Traversable and TraversableLike members
  //
  override def foreach[U](f: ((String, Any)) => U) {
    data.foreach(f)
  }

  override protected[this] def newBuilder: mutable.Builder[(String, Any), SessionCache] = {
    println("SESSIONCACHE newbuilder")
    new ListBuffer[(String, Any)]().mapResult(tuples => this.withData(tuples.toMap))
  }

  //
  // Private members
  //
  private def withData(newData: Map[String, Any]) = {
    new SessionCache(providedData=Some(newData), services=services)
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


case class SessionCacheServices @Inject() (
  sessionFactory: () => play.mvc.Scope.Session,
  appCache: ApplicationCache
)


object SessionCache extends Logging {
  // Uncomment this and finish the canBuildFrom implementation if you find that you need some of the
  // methods provided by having a custom CanBuildFrom implicit for the type (e.g. ++). See the
  // Traversable and TraversableLike scaladocs to see which methods benefit from having an
  // implicit CanBuildFrom. We've avoided it because, being static, it's impossible
  // to get the "right" Guice injector into the scope, so it would always have to be the default.
  //
  // If that confused the hell out of you like it did me, check out the following Gist:
  // https://gist.github.com/1136259
  //
  /*def cacheFact:SessionCacheFactory = null

  override protected[this] def newBuilder: mutable.Builder[(String, Any), SessionCache] = {
    new ListBuffer[(String, Any)]().mapResult(tuples => new SessionCache(null, null, withData(tuples.toMap)))
  }


  implicit def canBuildFrom = {
    new CanBuildFrom[Traversable[(String,Any)], (String,Any), SessionCache] {
      def apply() = newBuilder
      def apply(from: Traversable[(String,Int)]) = newBuilder
    }
  }*/
}


private[http] class SessionCacheFactory @Inject() (cacheServices:SessionCacheServices) extends (() => SessionCache) {
  def apply(): SessionCache = {
    new SessionCache(providedData=None, services=cacheServices)
  }
}
