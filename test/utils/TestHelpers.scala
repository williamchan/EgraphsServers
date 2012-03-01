package utils

import java.io.{File, FileInputStream}

object TestHelpers {

  def getStringFromFile(file: File): String = {
    val xmlIn: FileInputStream = new FileInputStream(file)
    val xmlBytes: Array[Byte] = new Array[Byte](xmlIn.available)
    xmlIn.read(xmlBytes)
    new String(xmlBytes)
  }

}
