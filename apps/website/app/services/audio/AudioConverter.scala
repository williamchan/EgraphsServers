package services.audio

import services.{SampleRateConverter, Utils, TempFile}
import com.xuggle.mediatool.ToolFactory
import services.blobs.Blobs
import java.io.File
import javax.sound.sampled.{AudioSystem, AudioFormat}

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
    // Ensure that WAV is in the correct format before passing to Xuggler's converter
    val sourceWav = SampleRateConverter.convert(44100f, sourceAudio)

    // save sourceAudio to temp file
    val sourceTempFile = TempFile.named(tempFilesId + "/audio.wav")
    val targetTempFile = TempFile.named(tempFilesId + "/audio.mp3")
    Utils.saveToFile(sourceWav, sourceTempFile)

    Utils.convertMediaFile(sourceTempFile, targetTempFile)
    val audioAsMp3 = Blobs.Conversions.fileToByteArray(targetTempFile)

    sourceTempFile.delete()
    targetTempFile.delete()
    audioAsMp3
  }

  def convertToAAC(sourceAudio: Array[Byte], tempFilesId: String): Array[Byte] = {
    // save sourceAudio to temp file
    val sourceTempFile = TempFile.named(tempFilesId + "/audio.mp3")
    val targetTempFile = TempFile.named(tempFilesId + "/audio.aac")
    Utils.saveToFile(sourceAudio, sourceTempFile)

    Utils.convertMediaFile(sourceTempFile, targetTempFile)
    val audioAsAAC = Blobs.Conversions.fileToByteArray(targetTempFile)

    sourceTempFile.delete()
    targetTempFile.delete()
    audioAsAAC
  }

  def getDurationOfWav(file: File): Int = {
    val format: AudioFormat = AudioSystem.getAudioInputStream(/*also accepts inputStream*/ file).getFormat
    (file.length / format.getSampleRate / format.getFrameSize / format.getChannels + 1).toInt
  }
}
