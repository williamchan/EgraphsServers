package utils

import java.io.{File, FileInputStream}
import services.blobs.Blobs
import play.Play

object TestHelpers {

  def fileAsBytes(filename: String): Array[Byte] = {
    Blobs.Conversions.fileToByteArray(Play.getFile(filename))
  }

  def getStringFromFile(file: File): String = {
    val xmlIn: FileInputStream = new FileInputStream(file)
    val xmlBytes: Array[Byte] = new Array[Byte](xmlIn.available)
    xmlIn.read(xmlBytes)
    new String(xmlBytes)
  }

  def getBlobKeyFromTestBlobUrl(testBlobUrl: String): String = {
    testBlobUrl.substring(testBlobUrl.indexOf("test/files/") + "test/files/".length)
  }

}
