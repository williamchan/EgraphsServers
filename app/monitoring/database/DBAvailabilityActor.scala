package monitoring.database

import akka.actor._
import play.api.libs.ws._
import play.api.libs.concurrent.Promise
import java.util.Date
import common.CloudWatchMetricPublisher
import collections.LimitedQueue
import collections.EgraphsMetric
import monitoring.ActorUtilities
import play.api.db.DB
import play.api.Play.current
import anorm._
import common.MonitoringMessages.CheckStatus
import common.MonitoringMessages.GetMetric

class DBAvailabilityActor(database: String, friendlyName: String,
  val publisher: CloudWatchMetricPublisher)
  extends Actor with ActorUtilities with ActorLogging {

  private val history = new LimitedQueue[Int](60)

  def receive() = {

    case CheckStatus => checkStatus
    case GetMetric => sender ! EgraphsMetric(friendlyName, database, history.toIndexedSeq)
    case _ => println("Cannot handle this message")
  }

  def checkStatus = {

    val dbConnections = sendDBQuery
    awsActions("DBAvailability." + database, dbConnections)
  }

  /*
   * If the database is down, CloudWatch will not receive data, given the current
   * implementation. As a result, it is appropriate to alarm in both the situation
   * "no data" and if the number of connections drops below a certain threshold.
   */
  def sendDBQuery: Int = {

    try {

      DB.withConnection(database) { implicit conn =>
        conn.setReadOnly(true)

        val result = SQL("select count(*) from pg_stat_activity").apply().head
        val count = result[Long]("count")

        // add to history
        history.enqueue(count.toInt)
        count.toInt
      }
    } catch {
      case _ => {
        history.enqueue(0)
        0
      }
    }
  }
}