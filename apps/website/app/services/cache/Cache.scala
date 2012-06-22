package services.cache

import com.google.inject.Inject
import java.io._
import play.cache.EhCacheImpl
import redis.clients.jedis.Jedis
import scala.Some
import services.logging.Logging
import services.Utils
import services.http.{PlayConfig}
import play.modules.redis.{RedisConnectionManager}
import java.util

private[cache] trait Cache {
  def set[T <: AnyRef](key: String, value: T, expirationSeconds: Int)
  def get[T <: AnyRef : Manifest](key: String): Option[T]
  def delete(key: String)
  def clear()
}


private[cache] class CacheFactory @Inject()(@PlayConfig playConfig: util.Properties)
  extends (() => Cache)
{
  import CacheFactory._

  def apply(): Cache = {
    playConfig.getProperty("application.cache") match {
      case "memory" =>
        inMemoryCache

      case "redis" =>
        try {
          redisCache
        } catch {
          case oops: Exception =>
            error("Unable to connect to redis cache instance. Falling back to in-memory cache")
            Utils.logException(oops)

            inMemoryCache
        }

      case unrecognized =>
        throw new IllegalArgumentException(
          "Unrecognized play configuration value for \"application.cache:\" " + unrecognized
        )
    }
  }

  //
  // Private members
  //
  private[cache] def inMemoryCache = {
    new InMemoryCache(EhCacheImpl.getInstance())
  }

  private[cache] def redisCache = {
    new RedisCache(RedisConnectionManager.getRawConnection)
  }
}


object CacheFactory extends Logging


private[cache] class RedisCache (jedis: Jedis) extends Cache {
  import RedisApplicationCache._
  import Utils.closing


  def set[T <: AnyRef](key: String, value: T, expirationSeconds: Int) {
    require(manifest.erasure.isInstanceOf[Serializable])
    val keyBytes = key.getBytes

    jedis.setex(keyBytes, expirationSeconds, toByteArray(value))

  }

  override def get[T <: AnyRef : Manifest](key: String): Option[T] = {
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

  def delete(key: String) {
    jedis.del(key)
  }

  //
  // Private members
  //
  private def fromByteArray[T <: AnyRef : Manifest](bytes: Array[Byte]): Option[T] = {
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

  private def toByteArray(toSerialize: AnyRef): Array[Byte] = {
    closing(new ByteArrayOutputStream()) { byteStream =>
      closing(new ObjectOutputStream(byteStream)) { objectStream =>
        objectStream.writeObject(toSerialize)

        byteStream.toByteArray
      }
    }
  }
}


private[cache] object RedisApplicationCache extends Logging


private[cache] class InMemoryCache @Inject() (cache: EhCacheImpl) extends Cache {
  import InMemoryCache._

  override def set[T <: AnyRef](key: String, value: T, expirationSeconds: Int) {
    cache.set(key, value, expirationSeconds)
  }

  override def get[T <: AnyRef : Manifest](key: String): Option[T] = {
    cache.get(key) match {
      case null => None
      case ourType: T => Some(ourType)
      case unexpectedObject =>
        error("Failed to deserialize object into a " +
          manifest.erasure.getName + ": " +
          unexpectedObject
        )
        None
    }
  }

  def clear() {
    cache.clear()
  }

  def delete(key: String) {
    cache.delete(key)
  }
}


private[cache] object InMemoryCache extends Logging
