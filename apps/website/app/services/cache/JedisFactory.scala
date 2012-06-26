package services.cache

import com.google.inject.Inject
import redis.clients.jedis.Jedis
import play.modules.redis.RedisConnectionManager
import services.Utils

/**
 * Factory for the lowest-level Redis connection.
 *
 * Throws up everywhere if it can't connect to a redis connection.
 */
private[cache] class JedisFactory @Inject()() {
  def apply(db: Int = JedisFactory.defaultRedisDb): Option[Jedis] = {
    val maybeJedis = try {
      Some(RedisConnectionManager.getRawConnection)
    } catch {
      case oops =>
        error("Unable to connect to redis cache instance.")
        Utils.logException(oops)

        None
    }

    // Select the correct database index
    maybeJedis.map {
      jedis =>
        jedis.select(db)

        jedis
    }
  }
}

object JedisFactory {
  private[cache] val defaultRedisDb = 5
}
