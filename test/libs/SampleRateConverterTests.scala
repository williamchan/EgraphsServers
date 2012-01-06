package libs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import javax.sound.sampled.{AudioSystem, AudioFormat}
import java.io._

class SampleRateConverterTests extends UnitFlatSpec
with ShouldMatchers {

  it should "downsample 44khz WAV to 4khz WAV" in {
    val path: File = new File("test/files")
    val sourceFile: File = new File(path, "44khz.wav")
    val targetFile: File = new File(path, "8khz.wav")

    val sourceFormat: AudioFormat = AudioSystem.getAudioInputStream(sourceFile).getFormat
    sourceFormat.getChannels should be(1)
    sourceFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    sourceFormat.getFrameRate should be(44100f)
    sourceFormat.getSampleRate should be(44100f)
    sourceFormat.getFrameSize should be(2)
    sourceFormat.getSampleSizeInBits should be(16)

    convertTestFile(8000f, sourceFile, targetFile)

    val targetFormat: AudioFormat = AudioSystem.getAudioInputStream(targetFile).getFormat
    targetFormat.getChannels should be(1)
    targetFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    targetFormat.getFrameRate should be(8000f)
    targetFormat.getSampleRate should be(8000f)
    targetFormat.getFrameSize should be(2)
    targetFormat.getSampleSizeInBits should be(16)
  }

  private def convertTestFile(targetSampleRate: Float, sourceFile: File, targetFile: File) {
    val bas: ByteArrayOutputStream = new ByteArrayOutputStream();
    val fs: FileInputStream = new FileInputStream(sourceFile);
    while (fs.available() > 0) {
      val fdata = fs.read();
      bas.write(fdata);
    }
    fs.close();

    val output = SampleRateConverter.convert(targetSampleRate, bas.toByteArray, /*debug*/ true)

    val out = new FileOutputStream(targetFile);
    out.write(output);
    out.close();
  }
}
