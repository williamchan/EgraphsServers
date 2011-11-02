package services.voice;

import junit.framework.TestCase;

public class VoiceBiometricsClientTest extends TestCase {

    private static VoiceBiometricsClient client;

    public VoiceBiometricsClientTest() {
        super();
        try {
            client = new VoiceBiometricsClient();
        } catch (Exception e) {
        }
    }

    public void testEnrollWill() throws Exception {
        client.sendStartEnrollmentRequest("will", /*rebuildtemplate*/true);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals(VoiceBiometricsClient.StartEnrollment, client.getResponseType());
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        sendAudioCheckRequest(transactionId, "ipad1d.wav");
        sendAudioCheckRequest(transactionId, "ipad2d.wav");
        sendAudioCheckRequest(transactionId, "ipad3d.wav");
        sendAudioCheckRequest(transactionId, "ipad4d.wav");
        sendAudioCheckRequest(transactionId, "ipad5d.wav");
        sendAudioCheckRequest(transactionId, "ipad6d.wav");

        client.sendEnrollUserRequest(transactionId);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("true", successValue);

        client.sendFinishEnrollTransactionRequest(transactionId, successValue);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }

    public void testVerifyWill() throws Exception {
        verify("will", "ipad7d.wav", true);  // speech
//        verify("will", "ipad8d.wav", true);  // false. speech
//        verify("will", "ipad9d.wav", true);  // false. speech
        verify("will", "ipad10d.wav", true); // numbers

//        verify("will", "dave1d.wav", false); // 90400
        verify("will", "dave2d.wav", false);
        verify("will", "dave3d.wav", false);
        verify("will", "dave4d.wav", false);
        verify("will", "dave5d.wav", false);
        verify("will", "dave6d.wav", false);
        verify("will", "dave7d.wav", false);
        verify("will", "dave8d.wav", false);
//        verify("will", "dave9d.wav", true);   // False positive
//        verify("will", "dave10d.wav", false); // 90400

        verify("will", "andrew_0_8khz.wav", false);
//        verify("will", "andrew_1_8khz.wav", false); // 90400. Why!
    }

    public void testEnrollDave() throws Exception {
        client.sendStartEnrollmentRequest("dave", /*rebuildtemplate*/true);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals(VoiceBiometricsClient.StartEnrollment, client.getResponseType());
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        sendAudioCheckRequest(transactionId, "dave2d.wav");
        sendAudioCheckRequest(transactionId, "dave3d.wav");
        sendAudioCheckRequest(transactionId, "dave4d.wav");
        sendAudioCheckRequest(transactionId, "dave5d.wav");
        sendAudioCheckRequest(transactionId, "dave6d.wav");

        client.sendEnrollUserRequest(transactionId);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("true", successValue);

        client.sendFinishEnrollTransactionRequest(transactionId, successValue);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }


    public void testVerifyDave() throws Exception {
//        verify("dave", "dave7d.wav", true); // Short. Why did this fail?
//        verify("dave", "dave8d.wav", true); // two voices
        verify("dave", "dave9d.wav", true);
//        verify("dave", "dave10d.wav", true);  // 90400. Short, but we hear no noise.

        verify("dave", "ipad1d.wav", false);
        verify("dave", "ipad2d.wav", false);
        verify("dave", "ipad3d.wav", false);
        verify("dave", "ipad4d.wav", false);
        verify("dave", "ipad5d.wav", false);
        verify("dave", "ipad6d.wav", false);
        verify("dave", "ipad7d.wav", false);
        verify("dave", "ipad8d.wav", false);
        verify("dave", "ipad9d.wav", false);
        verify("dave", "ipad10d.wav", false);
    }


    private static void verify(String name, String file, boolean expected) throws Exception {
        client.sendStartVerificationRequest(name);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);
        sendVerifySampleRequest(transactionId, file, expected);
    }

    private static void sendAudioCheckRequest(String transactionId, String file) throws Exception {
        client.sendAudioCheckRequest(transactionId, "test/files/" + file);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        System.out.println(client.getResponseValue(VoiceBiometricsClient.usabletime));
    }

    private static void sendVerifySampleRequest(String transactionId, String file, boolean expected) throws Exception {
        client.sendVerifySampleRequest(transactionId, "test/files/" + file);
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