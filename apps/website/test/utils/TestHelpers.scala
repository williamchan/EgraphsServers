package utils

import java.io.{File, FileInputStream}
import services.blobs.Blobs
import play.Play
import services.AppConfig
import org.jclouds.blobstore.domain.Blob

object TestHelpers {

  private val blobs = AppConfig.instance[Blobs]

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

}
