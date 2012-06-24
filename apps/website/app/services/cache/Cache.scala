package services.cache

import com.google.inject.Inject
import java.io._
import play.cache.EhCacheImpl
import redis.clients.jedis.Jedis
import scala.Some
import services.logging.Logging
import services.Utils
import play.modules.redis.RedisConnectionManager

/**
 * Trait for any class that implements a Cache in our system. Caches are simple key value stores.
 *
 * Usage:
 *
 * {{{
 *   class MyClass @Inject() (cacheFactory: () => Cache) {
 *     // Setting the object is simple. Make SURE that the object implements Serializable
 *     // or it will fail at runtime.
 *     def cache = cacheFactory()
 *     cache.set("a string", "herp", 1)
 *     cache.set("a number", 1234, 1)
 *     cache.set("a list", List(1,2,3,4), 1)
 *     cache.set("a map", Map("herp" -> "derp"), 100)
 *
 *     // Get an object by type-parameterizing the .get method. Be careful,
 *     // because if you get the type parameter wrong then it'll blow up
 *     // at run-time when you try to use the instance.
 *     println("string is: " + cache.get[String]("a string"))
 *     println("number is: " + cache.get[Number]("a number"))
 *     println("list is: " + cache.get[List[Int]]("a list"))
 *     println("map is: " + cache.get[Map[String, String]]("a map"))
 *
 *     // Delete the object
 *     cache.delete("a map")
 *   }
 * }}}
 */
private[cache] trait Cache {
  /**
   * Sets a Serializable object into the cache on the specified key for a particular amount of time.
   * Overwrites any existing values.
   *
   * The values you pass in must be Serializable or it will fail at run-time.
   *
   * @param key the key to insert.
   * @param value the value to insert. It can be any serializable type.
   * @param expirationSeconds the number of seconds before the key gets automatically
   *     flushed from the cache.
   * @tparam T type of the value.
   */
  def set[T](key: String, value: T, expirationSeconds: Int)

  /**
   * Retrieves a serializable object identified by the `key` from the cache, or
   * None if it wasn't found.
   *
   * Due to type-erasure, the type checking here is not and can not be strong.
   * So just make sure you never query out
   *
   * @param key the key to look up
   * @tparam T the type to get
   * @return Some(whatYouWereLookingFor) if found, otherwise None
   */
  def get[T : Manifest](key: String): Option[T]

  /**
   * Deletes the record mapped to the provided key, if a record was there.
   *
   * @param key key of the record to delete.
   */
  def delete(key: String)

  /**
   * Clears the entire cache. Avoid doing this except on an ad-hoc basis.
   */
  def clear()
}

/**
 * Yields the configured [[services.cache.Cache]] implementation.
 * Prefer accessing it by injecting a () => Cache.
 *
 * Implementation selection is governed by the play configuration "application.cache"
 * property. Valid values:
 *
 * - "redis": A Redis cache implementation against our hosted distributed cache. If the connection
 *       can't be made to the cache server then it defaults to the in-memory cache.
 * - "memory": An in-memory implementation that delegates to Play's [[play.cache.EhCacheImpl]]
 **/
private[cache] class CacheFactory @Inject()(
  utils: Utils,
  jedisFactory: JedisFactory
) extends (() => Cache)
{
  import CacheFactory._

  def apply(): Cache = {
    val appCacheValue = utils.requiredConfigurationProperty("application.cache")
    println("appCacheValue was " + appCacheValue.split("\\.").toList)

    appCacheValue.split("\\.").toList match {
      case List("memory") =>
        inMemoryCache

      case List("redis", dbNumber) =>
        redisCacheIfPossible(db=dbNumber.toInt)

      case List("redis") =>
        redisCacheIfPossible()

      case unrecognized =>
        throw new IllegalArgumentException(
          "Unrecognized play configuration value for \"application.cache:\" " + unrecognized
        )
    }
  }

  //
  // Private members
  //
  private[cache] def inMemoryCache: Cache = {
    new InMemoryCache(EhCacheImpl.getInstance())
  }

  private[cache] def redisCacheIfPossible(db: Int=JedisFactory.defaultRedisDb): Cache = {
    val maybeJedis = jedisFactory(db=db)
    val maybeRedisCache = maybeJedis.map(jedis => new RedisCache(jedis))

    maybeRedisCache.getOrElse {
      log("Falling back to in-memory cache due to failure to acquire redis connection")

      inMemoryCache
    }
  }

  private[cache] def redisCacheOrBust(db: Int=JedisFactory.defaultRedisDb): Cache = {
    jedisFactory(db).map(jedis => new RedisCache(jedis)).getOrElse {
      throw new Exception(
        "All we wanted was a redis cache implementation," +
        " but now we want to watch the world burn."
      )
    }
  }
}


object CacheFactory extends Logging


private[cache] class JedisFactory @Inject()() {
  def apply(db: Int=JedisFactory.defaultRedisDb): Option[Jedis] = {
    val maybeJedis = try {
      Some(RedisConnectionManager.getRawConnection)
    } catch {
      case oops =>
        error("Unable to connect to redis cache instance.")
        Utils.logException(oops)

        None
    }

    // Select the correct database index
    maybeJedis.map { jedis =>
      jedis.select(db)

      jedis
    }
  }
}

object JedisFactory {
  val defaultRedisDb = 5
}

/**
 * Cache implementation based on connection to our Redis server. Prefer getting
 * a Cache instance by injecting a () => Cache into your class.
 *
 * @param jedis the low-level Redis client, provided by
 *    [[play.modules.redis.RedisConnectionManager.getRawConnection]]
 */
private[cache] class RedisCache @Inject()(jedis: Jedis) extends Cache {
  import RedisApplicationCache._
  import Utils.closing

  //
  // Cache members
  //
  override def set[T](key: String, value: T, expirationSeconds: Int) {
    require(manifest.erasure.isInstanceOf[Serializable])
    val keyBytes = key.getBytes

    jedis.setex(keyBytes, expirationSeconds, toByteArray(value))

  }

  override def get[T : Manifest](key: String): Option[T] = {
    require(manifest.erasure.isInstanceOf[Serializable])
    val bytes = jedis.get(key.getBytes)

    if (bytes == null) {
      None
    } else {
      fromByteArray[T](bytes)
    }
  }

  override def clear() {
    jedis.flushDB()
  }

  override def delete(key: String) {
    jedis.del(key)
  }

  //
  // Private members
  //
  private def fromByteArray[T : Manifest](bytes: Array[Byte]): Option[T] = {
    try {
      closing(new ByteArrayInputStream(bytes)) { byteStream =>
        closing(new ObjectInputStream(byteStream)) { objectStream =>
          Some(objectStream.readObject().asInstanceOf[T])
        }
      }
    } catch {
      case e: Exception =>
        error("The following bytes failed to deserialize into a " + manifest.erasure.getName)
        error(new String(bytes, "UTF-8"))
        Utils.logException(e)
        e match {
          // All expected de-serialization exceptions produce None
          case _: ClassNotFoundException |
               _: InvalidClassException |
               _: StreamCorruptedException |
               _: OptionalDataException |
               _: IOException =>
            None

          // Other exceptions get the VIP treatment
          case unexpectedE =>
            throw unexpectedE
        }
    }
  }

  private def toByteArray(toSerialize: Any): Array[Byte] = {
    closing(new ByteArrayOutputStream()) { byteStream =>
      closing(new ObjectOutputStream(byteStream)) { objectStream =>
        objectStream.writeObject(toSerialize.asInstanceOf[AnyRef])

        byteStream.toByteArray
      }
    }
  }
}


private[cache] object RedisApplicationCache extends Logging


/**
 * Default in-memory cache implementation. Prefer getting a Cache
 * instance by injecting a () => Cache into your class.
 *
 * @param cache the EhCacheImpl from play.
 */
private[cache] class InMemoryCache @Inject() (cache: EhCacheImpl) extends Cache {

  override def set[T](key: String, value: T, expirationSeconds: Int) {
    cache.set(key, value.asInstanceOf[AnyRef], expirationSeconds)
  }

  override def get[T : Manifest](key: String): Option[T] = {
    // No, this is not type-safe. There's no way to make it so.
    Option(cache.get(key)).map(value => value.asInstanceOf[T])
  }

  override def clear() {
    cache.clear()
  }

  override def delete(key: String) {
    cache.delete(key)
  }
}


private[cache] object InMemoryCache extends Logging
