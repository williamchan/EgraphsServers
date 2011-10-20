package com.vocalect.client;

import junit.framework.TestCase;

public class VoiceBiometricsClientTest extends TestCase {

    public void testEnrollWill() throws Exception {
        VoiceBiometricsClient client = new VoiceBiometricsClient();

        client.sendStartEnrollmentRequest("will", true);
        String transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        assertEquals(VoiceBiometricsClient.StartEnrollment, client.getResponseType());
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);

        client.sendAudioCheckRequest(transactionId, "enroll1.wav");
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        System.out.println(client.getResponseValue(VoiceBiometricsClient.usabletime));

        client.sendAudioCheckRequest(transactionId, "enroll2.wav");
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        System.out.println(client.getResponseValue(VoiceBiometricsClient.usabletime));

        client.sendAudioCheckRequest(transactionId, "enroll3.wav");
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        System.out.println(client.getResponseValue(VoiceBiometricsClient.usabletime));

        client.sendEnrollUserRequest(transactionId);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        String successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("true", successValue);

        client.sendFinishEnrollTransactionRequest(transactionId, successValue);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));

        client.sendStartVerificationRequest("will");
        transactionId = client.getResponseValue(VoiceBiometricsClient.transactionid);
        String prompt = client.getResponseValue(VoiceBiometricsClient.prompt);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
        assertNotNull(transactionId);
        System.out.println("prompt = " + prompt);

        client.sendVerifySampleRequest(transactionId, "verify1.wav");
        String score = client.getResponseValue(VoiceBiometricsClient.score);
        successValue = client.getResponseValue(VoiceBiometricsClient.success);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));

        System.out.println("score = " + score);
        System.out.println("successValue = " + successValue);

        client.sendFinishVerifyTransactionRequest(transactionId, successValue, score);
        assertEquals("0", client.getResponseValue(VoiceBiometricsClient.errorcode));
    }

    public void testSendRequest_MissingRequestType() throws Exception {
        VoiceBiometricsClient client = new VoiceBiometricsClient();
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