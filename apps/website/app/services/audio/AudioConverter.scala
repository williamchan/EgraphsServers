package services.audio

import services.{Utils, TempFile}
import com.xuggle.mediatool.ToolFactory
import services.blobs.Blobs

object AudioConverter {

  /**
   * Uses Xuggle to convert a WAV and return an mp3 as a byte array. A temporary file location is required by this
   * implementation, but a future direction can be to bypass the usage of temporary files.
   * Xuggle source code is available at https://github.com/xuggle/xuggle-xuggler
   *
   * @param sourceAudio the source audio, which can be a WAV
   * @param tempFilesId a temporary file location required for this method to run. Temp files will be created and then
   *                    deleted at tempFilesId/audio.wav and tempFilesId/audio.mp3
   * @return bytes of the mp3 encoded from the source audio
   */
  def convertWavToMp3(sourceAudio: Array[Byte], tempFilesId: String): Array[Byte] = {
    // save sourceAudio to temp file
    val sourceTempFile = TempFile.named(tempFilesId + "/audio.wav")
    val targetTempFile = TempFile.named(tempFilesId + "/audio.mp3")
    Utils.saveToFile(sourceAudio, sourceTempFile)

    convertToMp3(sourceTempFile.getPath, targetTempFile.getPath)
    val audioAsMp3 = Blobs.Conversions.fileToByteArray(targetTempFile)

    sourceTempFile.delete()
    targetTempFile.delete()
    audioAsMp3
  }

  /**
   * Code taken from http://wiki.xuggle.com/MediaTool_Introduction. We should explore ways to convert to mp3 without
   * the use of files, which are required by ToolFactory.makeReader and ToolFactory.makeWriter.
   *
   * @param sourceTempFileLoc file location that contains the source audio
   * @param targetTempFileLoc target file location to store the converted mp3 data
   */
  private def convertToMp3(sourceTempFileLoc: String, targetTempFileLoc: String) {
    val reader = ToolFactory.makeReader(sourceTempFileLoc)
    val writer = ToolFactory.makeWriter(targetTempFileLoc, reader)
    reader.addListener(writer)
    while (reader.readPacket() == null) {}
  }
}
