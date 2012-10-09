package services.cache

import com.google.inject.Inject
import redis.clients.jedis.Jedis
import services.Utils
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig
import redis.clients.jedis.JedisCommands
import services.AppConfig
import services.logging.Logging
import services.config.ConfigFileProxy

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
  
  private lazy val config = AppConfig.instance[ConfigFileProxy]
  private lazy val host = config.redisHost
  private lazy val port = config.redisPort
  private lazy val password = config.redisPassword
  private lazy val timeout = 2000
  private[cache] val defaultRedisDb = 5
}
