package libs

import play.test.UnitFlatSpec
import org.scalatest.matchers.ShouldMatchers
import java.io.File
import javax.sound.sampled.{AudioSystem, AudioFormat}

class SampleRateConverterTests extends UnitFlatSpec
with ShouldMatchers {

  it should "downsample 44khz WAV to 4khz WAV" in {
    val path: File = new File("test/files")
    val source: File = new File(path, "44khz.wav")
    val sourceFormat: AudioFormat = AudioSystem.getAudioInputStream(source).getFormat
    sourceFormat.getChannels should be(1)
    sourceFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    sourceFormat.getFrameRate should be(44100f)
    sourceFormat.getSampleRate should be(44100f)
    sourceFormat.getFrameSize should be(2)
    sourceFormat.getSampleSizeInBits should be(16)

    val target: File = new File(path, "8khz.wav")
    SampleRateConverter.convert(8000f, source, target)
    val targetFormat: AudioFormat = AudioSystem.getAudioInputStream(target).getFormat
    targetFormat.getChannels should be(1)
    targetFormat.getEncoding should be(AudioFormat.Encoding.PCM_SIGNED)
    targetFormat.getFrameRate should be(8000f)
    targetFormat.getSampleRate should be(8000f)
    targetFormat.getFrameSize should be(2)
    targetFormat.getSampleSizeInBits should be(16)
  }

//  it should "helper to generate WAVs" in {
//    convert("ipad1.wav", "ipad1d.wav");
//    convert("ipad2.wav", "ipad2d.wav");
//    convert("ipad3.wav", "ipad3d.wav");
//    convert("ipad4.wav", "ipad4d.wav");
//    convert("ipad5.wav", "ipad5d.wav");
//    convert("ipad6.wav", "ipad6d.wav");
//    convert("ipad7.wav", "ipad7d.wav");
//    convert("ipad8.wav", "ipad8d.wav");
//    convert("ipad9.wav", "ipad9d.wav");
//    convert("ipad10.wav", "ipad10d.wav");
//
//    convert("dave1.wav", "dave1d.wav");
//    convert("dave2.wav", "dave2d.wav");
//    convert("dave3.wav", "dave3d.wav");
//    convert("dave4.wav", "dave4d.wav");
//    convert("dave5.wav", "dave5d.wav");
//    convert("dave6.wav", "dave6d.wav");
//    convert("dave7.wav", "dave7d.wav");
//    convert("dave8.wav", "dave8d.wav");
//    convert("dave9.wav", "dave9d.wav");
//    convert("dave10.wav", "dave10d.wav");
//  }

  def convert(sourceStr: String, targetStr: String) {
    val path: File = new File("test/files")
    val source: File = new File(path, sourceStr)
    val target: File = new File(path, targetStr)
    SampleRateConverter.convert(8000f, source, target)
  }
}
