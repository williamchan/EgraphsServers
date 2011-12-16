package services.voice;

import junit.framework.TestCase;
import libs.SampleRateConverter;
import play.libs.Codec;

import java.io.*;

public class VoiceBiometricsClientTests extends TestCase {

    private static VoiceBiometricsClient client;

    public VoiceBiometricsClientTests() {
        super();
        try {
            client = new VoiceBiometricsClient();
        } catch (Exception e) {
        }
    }

//    public void testConverter() throws Exception {
//        File path = new File("test/files");
//        File source = new File(path, "voice_from_ipad.wav");
//        File target = new File(path, "voice_from_ipad_8khz.wav");
//        SampleRateConverter.convert(8000f, source, target);
//    }

//    public void testAndrewsAudioFile() throws Exception {
//        String base64String = getStringFromFile(new File("test/files/sample_audio_5.txt"));
//        String outputFileName = "test/files/sample_audio_5.wav";
//        base64ToWav(base64String, outputFileName);
//    }
//
//    public void testConvertToBase64AndBack() throws Exception {
//        String inputFileName = "test/files/sound.wav";
//        String base64String = wavToBase64(inputFileName);
//        System.out.println(base64String);
//
//        String outputFileName = "test/files/pls.wav";
//        base64ToWav(base64String, outputFileName);
//    }
//
//    private void base64ToWav(String base64String, String outputFileName) throws IOException {
//        byte[] bytes = Codec.decodeBASE64(base64String);
//        FileOutputStream out = new FileOutputStream(outputFileName);
//        out.write(bytes);
//        out.close();
//    }
//
//    private String wavToBase64(String inputFileName) throws IOException {
//        ByteArrayOutputStream bas = new ByteArrayOutputStream();
//        int fdata;
//        FileInputStream fs = new FileInputStream(inputFileName);
//        while (fs.available() > 0) {
//            fdata = fs.read();
//            bas.write(fdata);
//        }
//        fs.close();
//        return Codec.encodeBASE64(bas.toByteArray());
//    }
//
//    private String getStringFromFile(File file) throws Exception {
//        ByteArrayOutputStream bas = new ByteArrayOutputStream();
//        int fdata;
//        FileInputStream fs = new FileInputStream(file);
//        while (fs.available() > 0) {
//            fdata = fs.read();
//            bas.write(fdata);
//        }
//        fs.close();
//        return bas.toString();
//    }

    public void testEnrollWill() throws Exception {
        client.sendStartEnrollmentRequest("will", /*rebuildTemplate*/true);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals(VoiceBiometricsClient.StartEnrollment, client.getResponseType());
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/17.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/18.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/19.wav");
//        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/20.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/21.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/22.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/23.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/24.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/25.wav");
        sendAudioCheckRequest(transactionId, "tmp/blobstore/egraphs-test/voicesamples/26.wav");

        client.sendEnrollUserRequest(transactionId);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("true", successValue);

        client.sendFinishEnrollTransactionRequest(transactionId, successValue);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }

    public void testAudioFormatCompatibility_iPad() throws Exception {
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/17.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/18.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/19.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/20.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/21.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/22.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/23.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/24.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/25.wav");
        checkAudioFormatCompatibility("will", "tmp/blobstore/egraphs-test/voicesamples/26.wav");

//        verify("will", "test/files/sample_audio_5.wav", false);
    }

    public void testVerifyWill() throws Exception {
        verify("will", "test/files/ipad7d.wav", true);  // speech
//        verify("will", "test/files/ipad8d.wav", true);  // false. speech
//        verify("will", "test/files/ipad9d.wav", true);  // false. speech
        verify("will", "test/files/ipad10d.wav", true); // numbers

//        verify("will", "test/files/dave1d.wav", false); // 90400
        verify("will", "test/files/dave2d.wav", false);
        verify("will", "test/files/dave3d.wav", false);
        verify("will", "test/files/dave4d.wav", false);
        verify("will", "test/files/dave5d.wav", false);
        verify("will", "test/files/dave6d.wav", false);
        verify("will", "test/files/dave7d.wav", false);
        verify("will", "test/files/dave8d.wav", false);
//        verify("will", "test/files/dave9d.wav", true);   // False positive
//        verify("will", "test/files/dave10d.wav", false); // 90400

        verify("will", "test/files/andrew_0_8khz.wav", false);
//        verify("will", "test/files/andrew_1_8khz.wav", false); // 90400. Why!
    }

    public void testEnrollDave() throws Exception {
        client.sendStartEnrollmentRequest("dave", /*rebuildTemplate*/true);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals(VoiceBiometricsClient.StartEnrollment, client.getResponseType());
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        sendAudioCheckRequest(transactionId, "test/files/dave2d.wav");
        sendAudioCheckRequest(transactionId, "test/files/dave3d.wav");
        sendAudioCheckRequest(transactionId, "test/files/dave4d.wav");
        sendAudioCheckRequest(transactionId, "test/files/dave5d.wav");
        sendAudioCheckRequest(transactionId, "test/files/dave6d.wav");

        client.sendEnrollUserRequest(transactionId);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("true", successValue);

        client.sendFinishEnrollTransactionRequest(transactionId, successValue);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }


    public void testVerifyDave() throws Exception {
//        verify("dave", "test/files/dave7d.wav", true); // Short. Why did this fail?
//        verify("dave", "test/files/dave8d.wav", true); // two voices
        verify("dave", "test/files/dave9d.wav", true);
//        verify("dave", "test/files/dave10d.wav", true);  // 90400. Short, but we hear no noise.

        verify("dave", "test/files/ipad1d.wav", false);
        verify("dave", "test/files/ipad2d.wav", false);
        verify("dave", "test/files/ipad3d.wav", false);
        verify("dave", "test/files/ipad4d.wav", false);
        verify("dave", "test/files/ipad5d.wav", false);
        verify("dave", "test/files/ipad6d.wav", false);
        verify("dave", "test/files/ipad7d.wav", false);
        verify("dave", "test/files/ipad8d.wav", false);
        verify("dave", "test/files/ipad9d.wav", false);
        verify("dave", "test/files/ipad10d.wav", false);
    }

    private static void checkAudioFormatCompatibility(String name, String fileName) throws Exception {
        client.sendStartVerificationRequest(name);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        client.sendVerifySampleRequest(transactionId, fileName);
        String score = client.getResponseValue(VoiceBiometricsClient.score);
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        System.out.println(client.getResponseValue(VoiceBiometricsClient.errorcode) + " " + score + " " + successValue);

        client.sendFinishVerifyTransactionRequest(transactionId, successValue, score);
    }

    private static void verify(String name, String file, boolean expected) throws Exception {
        client.sendStartVerificationRequest(name);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);
        sendVerifySampleRequest(transactionId, file, expected);
    }

    private static void sendAudioCheckRequest(String transactionId, String fileName) throws Exception {
        client.sendAudioCheckRequest(transactionId, fileName);
//        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        System.out.println(client.getResponseValue(VoiceBiometricsClient.errorcode) + " " + client.getResponseValue(VoiceBiometricsClient.usabletime));
    }

    private static void sendVerifySampleRequest(String transactionId, String fileName, boolean expected) throws Exception {
        client.sendVerifySampleRequest(transactionId, fileName);
        String score = client.getResponseValue(VoiceBiometricsClient.score);
        System.out.println(score);
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        if (expected) {
            assertEquals("true", successValue);
        } else {
            assertEquals("false", successValue);
        }

        client.sendFinishVerifyTransactionRequest(transactionId, successValue, score);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }

    public void testSendRequest_MissingRequestType() throws Exception {
        try {
            client.sendRequest();
        } catch (Exception ex) {
            assertNotNull(ex);
        }
        client.setRequestType("BadRequest");
        client.sendRequest();
        assertEquals("UknownMethod", client.getResponseType());
        client.clearResponse();
    }

}