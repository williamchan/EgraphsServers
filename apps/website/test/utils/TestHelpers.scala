package utils

import java.io.{File, FileInputStream}
import services.blobs.Blobs
import play.Play
import services.AppConfig
import org.jclouds.blobstore.domain.Blob
import akka.actor.{Actor, ActorRef}
import akka.actor.Actor.actorOf

object TestHelpers {

  private val blobs = AppConfig.instance[Blobs]

  def putPublicImageOnBlobStore() {
    import Blobs.Conversions._
    blobs.put("a/b/derp.jpg", Play.getFile("./test/files/derp.jpg"))
  }

  def fileAsBytes(filename: String): Array[Byte] = {
    Blobs.Conversions.fileToByteArray(Play.getFile(filename))
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
  def withActorUnderTest[ActorT <: Actor: Manifest, ResultT]
  (actorInstance: => ActorT)(operation: ActorRef => ResultT): ResultT =
  {
    val actor = actorOf(actorInstance).start()

    executeOperationThenStop(actor)(operation)
  }

  /**
   * Convenience method that starts the actor, executes the operation, and stops the actor.
   */
  def withActorUnderTest[ActorT <: Actor: Manifest, ResultT]
  (operation: ActorRef => ResultT): ResultT =
  {
    val actor = actorOf[ActorT].start()

    executeOperationThenStop(actor)(operation)
  }

  private[this] def executeOperationThenStop[ActorT <: Actor: Manifest, ResultT]
  (actor: ActorRef)(operation: ActorRef => ResultT): ResultT =
  {
    try {
      operation(actor)
    }
    finally {
      actor.stop()
    }
  }

}
