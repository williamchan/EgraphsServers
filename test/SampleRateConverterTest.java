import junit.framework.TestCase;
import libs.SampleRateConverter;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import java.io.File;

public class SampleRateConverterTest extends TestCase {

    public void testConverter() throws Exception {
        File path = new File("test");
        File source = new File(path, "44khz.wav");
        AudioFormat sourceFormat = AudioSystem.getAudioInputStream(source).getFormat();
        assertEquals(1, sourceFormat.getChannels());
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, sourceFormat.getEncoding());
        assertEquals(44100f, sourceFormat.getFrameRate());
        assertEquals(44100f, sourceFormat.getSampleRate());
        assertEquals(2, sourceFormat.getFrameSize());
        assertEquals(16, sourceFormat.getSampleSizeInBits());

        File target = new File(path, "8khz.wav");
        SampleRateConverter.convert(8000f, source, target);
        AudioFormat targetFormat = AudioSystem.getAudioInputStream(target).getFormat();
        assertEquals(1, targetFormat.getChannels());
        assertEquals(AudioFormat.Encoding.PCM_SIGNED, targetFormat.getEncoding());
        assertEquals(8000f, targetFormat.getFrameRate());
        assertEquals(8000f, targetFormat.getSampleRate());
        assertEquals(2, targetFormat.getFrameSize());
        assertEquals(16, targetFormat.getSampleSizeInBits());
    }

    public void testGenerateWAVs() throws Exception {
        convert("will_enroll_0_44khz.wav", "will_enroll_0_8khz.wav");
        convert("will_enroll_1_44khz.wav", "will_enroll_1_8khz.wav");
        convert("will_enroll_2_44khz.wav", "will_enroll_2_8khz.wav");
        convert("will_enroll_3_44khz.wav", "will_enroll_3_8khz.wav");
        convert("will_enroll_4_44khz.wav", "will_enroll_4_8khz.wav");
        convert("will_enroll_5_44khz.wav", "will_enroll_5_8khz.wav");

        convert("will_verify_44khz.wav", "will_verify_8khz.wav");

        convert("andrew_0_44khz.wav", "andrew_0_8khz.wav");
        convert("andrew_1_44khz.wav", "andrew_1_8khz.wav");
    }

    public void testIPad() throws Exception {
        convert("ipad1.wav", "ipad1d.wav");
        convert("ipad2.wav", "ipad2d.wav");
        convert("ipad3.wav", "ipad3d.wav");
        convert("ipad4.wav", "ipad4d.wav");
        convert("ipad5.wav", "ipad5d.wav");
        convert("ipad6.wav", "ipad6d.wav");
        convert("ipad7.wav", "ipad7d.wav");
        convert("ipad8.wav", "ipad8d.wav");
        convert("ipad9.wav", "ipad9d.wav");
        convert("ipad10.wav", "ipad10d.wav");

        convert("dave1.wav", "dave1d.wav");
        convert("dave2.wav", "dave2d.wav");
        convert("dave3.wav", "dave3d.wav");
        convert("dave4.wav", "dave4d.wav");
        convert("dave5.wav", "dave5d.wav");
        convert("dave6.wav", "dave6d.wav");
        convert("dave7.wav", "dave7d.wav");
        convert("dave8.wav", "dave8d.wav");
        convert("dave9.wav", "dave9d.wav");
        convert("dave10.wav", "dave10d.wav");
    }

    private static void convert(String sourceStr, String targetStr) throws Exception {
        File path = new File("test/files");
        File source = new File(path, sourceStr);
        File target = new File(path, targetStr);
        SampleRateConverter.convert(8000f, source, target);
    }
}
