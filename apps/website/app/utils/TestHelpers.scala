package utils

import java.io.{File, FileInputStream}
import services.blobs.Blobs
import play.api.Play
import services.AppConfig
import java.io.File
import org.jclouds.blobstore.domain.Blob
import akka.actor.{Actor, ActorRef}
import play.api.libs.concurrent.Akka
import play.api.Application
import play.api.Play.current
import akka.actor.Props

object TestHelpers {

  private lazy val blobs = AppConfig.instance[Blobs]

  def putPublicImageOnBlobStore() {
    import Blobs.Conversions._
    blobs.put("a/b/derp.jpg", current.getFile("test/resources/derp.jpg"))
  }

  def fileAsBytes(filename: String): Array[Byte] = {
    Blobs.Conversions.fileToByteArray(current.getFile(filename))
  }

  def getStringFromFile(file: File): String = {
    val inputStream: FileInputStream = new FileInputStream(file)
    val bytes: Array[Byte] = new Array[Byte](inputStream.available)
    inputStream.read(bytes)
    new String(bytes)
  }

  def getBlobKeyFromTestBlobUrl(testBlobUrl: String): String = {
    testBlobUrl.substring(testBlobUrl.indexOf("blob/files/") + "blob/files/".length)
  }

  def getBlobFromTestBlobUrl(testBlobUrl: String): Option[Blob] = {
    val blobKey = getBlobKeyFromTestBlobUrl(testBlobUrl)
    blobs.get(blobKey)
  }

  /**
   * @return the baseUrl and a list of the query parameters as field-value tuples
   */
  def splitUrl(url: String): (String, List[(String, String)]) = {
    val urlAndQuery = url.split('?').toList
    val urlRoot = urlAndQuery.head
    val queryStringParams = urlAndQuery.tail.headOption match {
      case Some(query) => {
        query.split('&').toList.map(param => {
          val fieldAndValue = param.split('=')
          (fieldAndValue(0), fieldAndValue(1))
        })
      }
      case None => List.empty[(String, String)]
    }

    (urlRoot, queryStringParams)
  }

  /**
   * Convenience method that starts the actor, executes the operation, and stops the actor.
   */
  def withActorUnderTest[ActorT <: Actor, ResultT]
  (actorInstance: => ActorT)(testcase: ActorRef => ResultT)(implicit app: Application) : ResultT =
  {
    val actor = Akka.system.actorOf(Props(actorInstance))

    try {
      testcase(actor)
    } finally {
      Akka.system.stop(actor)
    }
  }
}
