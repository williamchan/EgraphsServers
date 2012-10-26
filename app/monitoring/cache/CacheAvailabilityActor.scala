package monitoring.cache

import akka.actor.Actor
import akka.actor.ActorLogging
import akka.actor.actorRef2Scala
import collections.EgraphsMetric
import collections.LimitedQueue
import common.CloudWatchMetricPublisher
import common.MonitoringMessages.CheckStatus
import common.MonitoringMessages.GetMetric
import monitoring.ActorUtilities
import play.api.Play
import redis.clients.jedis.JedisPool
import redis.clients.jedis.JedisPoolConfig

class CacheAvailabilityActor(url: String, friendlyName: String,
  val publisher: CloudWatchMetricPublisher)
  extends Actor with ActorUtilities with ActorLogging {

  // ** turn any connection errors into failures **

  private val history = new LimitedQueue[Int](60)

  def receive() = {

    case CheckStatus => checkStatus
    case GetMetric => sender ! EgraphsMetric(friendlyName, url, history.toIndexedSeq)
    case _ => println("Cannot handle this message")
  }

  def checkStatus = {

    val webResponse = checkCacheConnections
    //play.Logger.info("ABOUT TO SEND VALUE " + webResponse + " TO CLOUDWATCH, NOT FOR REAL")
    awsActions("RedisAvailability", webResponse)
  }

  def checkCacheConnections: Int = {

    val info = jedisPoolInfo

    val iter = info.split("\n")
    for (line <- iter) {
      if (line.startsWith("connected_clients")) {
        val parts = line.split(":")
        val numConnections = parts(1).trim().toInt
        history.enqueue(numConnections)
        return numConnections
      }
    }
    /** if no value found for "connected_clients", report failure */
    history.enqueue(0)
    return 0
  }

  private def jedisPoolInfo: String = {
    val poolConfig = new JedisPoolConfig
    poolConfig.setMaxActive(1)

    val host = Play.current.configuration.getString("redis.host")
      .getOrElse("unknown host")
    val port = Play.current.configuration.getInt("redis.port").getOrElse(0)
    val password = Play.current.configuration.getString("redis.password")
      .getOrElse("unknown password")
    val timeout = 2000

    play.Logger.info("Creating redis pool for host at " + host + ":" + port)
    val pool = new JedisPool(poolConfig, host, port, timeout, password)

    val poolResource = pool.getResource()
    val info = poolResource.info()
    pool.returnResource(poolResource)
    pool.destroy()

    return info
  }
}