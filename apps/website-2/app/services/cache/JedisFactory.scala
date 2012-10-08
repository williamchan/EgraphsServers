package services.cache

import com.google.inject.Inject
import redis.clients.jedis.Jedis
import services.Utils
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisCommands
import services.AppConfig
import play.api.Configuration
import services.logging.Logging

/**
 * Factory for the lowest-level Redis connection.
 *
 * Throws up everywhere if it can't connect to a redis connection.
 */
private[cache] class JedisFactory(db: Int) {
  import JedisFactory.jedisPool
  
  def connected[A](operation: Jedis => A): A = {
    val jedis = jedisPool.getResource()
    try {
      jedis.select(JedisFactory.defaultRedisDb)
      operation(jedis)
    } finally {
      jedisPool.returnResource(jedis)
    }
  }
}

object JedisFactory extends Logging {
  def startup() {
    jedisPool
  }
  
  def shutDown() {
    jedisPool.destroy()
  }
  
  //
  // Private members
  //
  private lazy val jedisPool = {
    val poolConfig = new JedisPoolConfig
    
    poolConfig.setTestOnBorrow(true)
    poolConfig.setMaxActive(-1)
    poolConfig.setMaxIdle(-1)
    
    log("Creating redis pool for host at " + host + ":" + port)
    new JedisPool(poolConfig, host, port, timeout, password)
  }
  
  private lazy val config = AppConfig.instance[Configuration]
  private lazy val host = config.getString("redis.host").getOrElse(throw new IllegalArgumentException("redis.host configuration required"))
  private lazy val port = config.getInt("redis.port").getOrElse(6379)
  private lazy val timeout = config.getInt("redis.timeout").getOrElse(2000)
  private lazy val password = config.getString("redis.password").getOrElse(throw new IllegalArgumentException("Redis password required"))
  private[cache] val defaultRedisDb = 5
}
