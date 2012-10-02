package services.cache

import com.google.inject.Inject
import redis.clients.jedis.Jedis
import services.Utils

/**
 * Factory for the lowest-level Redis connection.
 *
 * Throws up everywhere if it can't connect to a redis connection.
 */
private[cache] class JedisFactory @Inject()() {
  def apply(db: Int = JedisFactory.defaultRedisDb): Option[Jedis] = {
    // Select the correct database index
    maybeJedisConnection.map {
      jedis =>
        jedis.select(db)

        jedis
    }
  }

  //
  // Private members
  //
  def maybeJedisConnection: Option[Jedis] = {
    // TODO: PLAY20. Hey how about making this shit compile when you have a better Redis pool solution?
    /*try  {
      Some(RedisConnectionManager.getRawConnection)
    } catch {
      case oops =>
        error("Unable to connect to redis cache instance.")
        Utils.logException(oops)

        None
    }*/
  }
}

object JedisFactory {
  private[cache] val defaultRedisDb = 5
}
