package com.vocalect.client;

import org.apache.commons.codec.binary.Base64;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Hashtable;

public class VoiceBiometricsClient {
    private String requesttype = "";
    private String responsetype = "";
    private Hashtable<String, String> params = new Hashtable<String, String>();
    private Hashtable<String, String> responsevalues = new Hashtable<String, String>();

    private URL url = new URL("https://service03.voicebiogroup.com/service/xmlapi");
    private static String myClientname = "celebritydev";
    private static String myClientkey = "62ed7855e0af30d0af534ce195845c7f";

    public static String StartEnrollment = "StartEnrollment";
    public static String FinishTransaction = "FinishTransaction";
    public static String AudioCheck = "AudioCheck";
    public static String EnrollUser = "EnrollUser";
    public static String StartVerification = "StartVerification";
    public static String VerifySample = "VerifySample";
//    public static String RenameUser = "RenameUser";
//    public static String CheckUserStatus = "CheckUserStatus";
//    public static String SetUserStatus = "SetUserStatus"; // {“active”, “inactive”, “locked”, “opted-out”, “deleted”}

    public static String clientkey = "clientkey";
    public static String clientname = "clientname";
    public static String errorcode = "errorcode";
    public static String prompt = "prompt";
    public static String rebuildtemplate = "rebuildtemplate";
    public static String score = "score";
    public static String success = "success";
    public static String transactionid = "transactionid";
    public static String voicesample = "voicesample";
    public static String usabletime = "usabletime";
    public static String userid = "userid";

    public VoiceBiometricsClient() throws Exception {
    }

    public void sendStartEnrollmentRequest(String userid, boolean rebuildtemplate) throws Exception {
        clearParameters();
        setRequestType(VoiceBiometricsClient.StartEnrollment);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.userid, userid);
        setParameter(VoiceBiometricsClient.rebuildtemplate, Boolean.valueOf(rebuildtemplate).toString());
        sendRequest();
    }

    public void sendAudioCheckRequest(String transactionId, String filename) throws Exception {
        clearParameters();
        String voicesample = VoiceBiometricsClient.getVoicesampleBase64Encoded(filename);
        setRequestType(VoiceBiometricsClient.AudioCheck);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.transactionid, transactionId);
        setParameter(VoiceBiometricsClient.voicesample, voicesample);
        sendRequest();
    }

    public void sendEnrollUserRequest(String transactionId) throws Exception {
        clearParameters();
        setRequestType(VoiceBiometricsClient.EnrollUser);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.transactionid, transactionId);
        sendRequest();
    }

    public void sendFinishEnrollTransactionRequest(String transactionId, String successValue) throws Exception {
        clearParameters();
        setRequestType(VoiceBiometricsClient.FinishTransaction);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.transactionid, transactionId);
        setParameter(VoiceBiometricsClient.success, successValue);
        sendRequest();
    }

    public void sendFinishVerifyTransactionRequest(String transactionId, String successValue, String score) throws Exception {
        clearParameters();
        setRequestType(VoiceBiometricsClient.FinishTransaction);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.transactionid, transactionId);
        setParameter(VoiceBiometricsClient.success, successValue);
        setParameter(VoiceBiometricsClient.score, score);
        sendRequest();
    }

    public void sendStartVerificationRequest(String userid) throws Exception {
        clearParameters();
        setRequestType(VoiceBiometricsClient.StartVerification);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.userid, userid);
        sendRequest();
    }

    public void sendVerifySampleRequest(String transactionId, String filename) throws Exception {
        clearParameters();
        String voicesample = VoiceBiometricsClient.getVoicesampleBase64Encoded(filename);
        setRequestType(VoiceBiometricsClient.VerifySample);
        setParameter(VoiceBiometricsClient.clientname, VoiceBiometricsClient.myClientname);
        setParameter(VoiceBiometricsClient.clientkey, VoiceBiometricsClient.myClientkey);
        setParameter(VoiceBiometricsClient.transactionid, transactionId);
        setParameter(VoiceBiometricsClient.voicesample, voicesample);
        sendRequest();
    }

    public void clearParameters() {
        requesttype = "";
        params.clear();
    }

    public void clearResponse() {
        responsetype = "";
        responsevalues.clear();
    }

    public void setParameter(String paramname, String paramvalue) {
        if (paramname != null && paramvalue != null) {
            params.put(paramname, paramvalue);
        }
    }

    public void setRequestType(String requesttype) {
        if (requesttype != null) {
            this.requesttype = requesttype;
        }
    }

    public String getResponseValue(String valuename) {
        return responsevalues.get(valuename);
    }

    public String getResponseType() {
        return responsetype;
    }

    public void sendRequest(String fieldname) throws Exception {
        // Sends request using internal parameters and sets internal return values
        // Accepts a field name to use for the message (can be null)

        clearResponse();
        String xml = buildXMLRequest();
        // Make sure to encode the output stream for any weird characters
        xml = URLEncoder.encode(xml, "UTF-8");
        if (fieldname != null) {
            xml = fieldname + "=" + xml;
        }
        HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
        httpconn.setRequestMethod("POST");
        httpconn.setRequestProperty("Content-Length", String.valueOf(xml.length()));
        httpconn.setDoInput(true);
        httpconn.setDoOutput(true);
        httpconn.connect();
        OutputStream os = httpconn.getOutputStream();
        os.write(xml.getBytes());
        os.flush();

        // Process result
        BufferedReader reader = new BufferedReader(new InputStreamReader(httpconn.getInputStream()));
        String s;
        StringBuilder sb = new StringBuilder();
        while ((s = reader.readLine()) != null) {
            sb.append(s + "\n");
        }
        parseXMLResponse(sb.toString());
    }

    public void sendRequest() throws Exception {
        sendRequest(null);
    }

    // ================================ private helpers

    private static String getVoicesampleBase64Encoded(String filename) throws IOException {
        ByteArrayOutputStream bas = new ByteArrayOutputStream();
        int fdata;
        FileInputStream fs = new FileInputStream(filename);
        while (fs.available() > 0) {
            fdata = fs.read();
            bas.write(fdata);
        }
        fs.close();
        return Base64.encodeBase64String(bas.toByteArray());
    }

    private String buildXMLRequest() throws Exception {
        StringBuilder xml = new StringBuilder();
        String field;

        if (requesttype.equals("")) {
            throw new Exception();
        }
        xml.append("<" + requesttype + ">\n");
        Enumeration<String> fields = params.keys();
        while (fields.hasMoreElements()) {
            field = fields.nextElement();
            xml.append("<" + field + ">" + params.get(field) + "</" + field + ">\n");
        }
        xml.append("</" + requesttype + ">\n");
        return xml.toString();
    }

    // Processes response XML and stuffs resulting values into response hashtable
    private void parseXMLResponse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docbuilder = factory.newDocumentBuilder();
        Document doc = docbuilder.parse(new InputSource(new StringReader(xml)));
        String docType = doc.getDocumentElement().getTagName();
        Node node = doc.getFirstChild();
        NodeList nodes = node.getChildNodes();

        responsetype = docType;
        for (int i = 0; i < nodes.getLength(); i++) {
            node = nodes.item(i);
            if (node.getNodeType() == Node.ELEMENT_NODE) {
                responsevalues.put(node.getNodeName(), node.getTextContent());
            }
        }
    }

}