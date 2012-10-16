package services

import blobs.Blobs
import javax.sound.sampled.{AudioSystem, AudioFormat}
import java.io._
import utils.EgraphsUnitTest

class SampleRateConverterTests extends EgraphsUnitTest {  

  it should "downsample 44kHz WAV to 8kHz WAV" in new EgraphsTestApplication {
    val sourceFile: File = resourceFile("44khz.wav")
    val targetFile: File = resourceFile("8khz.wav")

    val sourceFormat: AudioFormat = AudioSystem.getAudioInputStream(sourceFile).getFormat
    sourceFormat.getChannels should be(1)
    sourceFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    sourceFormat.getFrameRate should be(44100f)
    sourceFormat.getSampleRate should be(44100f)
    sourceFormat.getFrameSize should be(2)
    sourceFormat.getSampleSizeInBits should be(16)

    val inputBytes: Array[Byte] = Blobs.Conversions.fileToByteArray(sourceFile)
    val outputBytes = SampleRateConverter.convert(8000f, inputBytes, /*debug*/ true)
    val out = new FileOutputStream(targetFile)
    out.write(outputBytes)
    out.close()

    val targetFormat: AudioFormat = AudioSystem.getAudioInputStream(targetFile).getFormat
    targetFormat.getChannels should be(1)
    targetFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    targetFormat.getFrameRate should be(8000f)
    targetFormat.getSampleRate should be(8000f)
    targetFormat.getFrameSize should be(2)
    targetFormat.getSampleSizeInBits should be(16)
  }

  it should "not change 8kHz WAV when converting it to 8kHz WAV" in new EgraphsTestApplication {
    val sourceFile: File = resourceFile("8khz.wav")

    val sourceFormat: AudioFormat = AudioSystem.getAudioInputStream(sourceFile).getFormat
    sourceFormat.getChannels should be(1)
    sourceFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    sourceFormat.getFrameRate should be(8000f)
    sourceFormat.getSampleRate should be(8000f)
    sourceFormat.getFrameSize should be(2)
    sourceFormat.getSampleSizeInBits should be(16)

    val inputBytes: Array[Byte] = Blobs.Conversions.fileToByteArray(sourceFile)
    val outputBytes = SampleRateConverter.convert(8000f, inputBytes, /*debug*/ true)
    outputBytes should be(inputBytes)
  }
}
