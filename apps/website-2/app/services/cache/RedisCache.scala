package services.cache

import services.logging.Logging
import java.io._
import scala.Some
import services.Utils
import com.google.inject.Inject
import redis.clients.jedis.Jedis
import services.config.ConfigFileProxy

/**
 * Cache implementation based on connection to our Redis server. Prefer getting
 * a Cache instance by injecting a CacheFactory into your class and using its
 * applicationCache instance.
 *
 * @param jedis the low-level Redis client.
 */
private[cache] class RedisCache @Inject()(pool: JedisFactory, config: ConfigFileProxy) extends Cache {

  import RedisCache._
  import Utils.closing

  //
  // Cache members
  //
  override def set[T](key: String, value: T, expirationSeconds: Int) {
    require(manifest.erasure.isInstanceOf[Serializable])
    val keyBytes = key.getBytes
    
    pool.connected(jedis => jedis.setex(keyBytes, expirationSeconds, toByteArray(value)))
  }

  override def get[T: Manifest](key: String): Option[T] = {
    require(manifest.erasure.isInstanceOf[Serializable])
    val bytes = pool.connected(jedis => jedis.get(key.getBytes))

    if (bytes == null) {
      None
    } else {
      fromByteArray[T](bytes)
    }
  }

  override def clear() {
    require(config.applicationMode == "dev", "Application mode must be dev to scrub the cache.")

    pool.connected(jedis => jedis.flushDB())
  }

  override def delete(key: String) {
    pool.connected(jedis => jedis.del(key))
  }

  //
  // Private members
  //
  private def fromByteArray[T: Manifest](bytes: Array[Byte]): Option[T] = {
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

private[cache] object RedisCache extends Logging