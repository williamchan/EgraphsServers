package services.voice

import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import org.xml.sax.InputSource
import org.w3c.dom.{Node, Document}
import java.util.Hashtable
import java.net.{URLEncoder, URL}
import java.io._
import play.libs.Codec
import libs.{Blobs, SampleRateConverter}
import Blobs.Conversions._
import javax.net.ssl.HttpsURLConnection

class VBGRequest {
  private var requestType: String = ""
  private var responseType: String = ""
  private val requestParams: Hashtable[String, String] = new Hashtable[String, String]
  private val responseValues: Hashtable[String, String] = new Hashtable[String, String]

  def sendRequest(): VBGRequest = {
    // Sends request using internal parameters and sets internal return values
    // Accepts a field name to use for the message (can be null)

    var xml: String = buildXMLRequest
    // Make sure to encode the output stream for any weird characters
    xml = URLEncoder.encode(xml, "UTF-8")
    val httpConn: HttpsURLConnection = VBGBiometricServices._url.openConnection.asInstanceOf[HttpsURLConnection]
    httpConn.setRequestMethod("POST")
    httpConn.setRequestProperty("Content-Length", String.valueOf(xml.length))
    httpConn.setDoInput(true)
    httpConn.setDoOutput(true)
    httpConn.connect()
    val os: OutputStream = httpConn.getOutputStream
    os.write(xml.getBytes)
    os.flush()

    // Process result
    val reader: BufferedReader = new BufferedReader(new InputStreamReader(httpConn.getInputStream))
    var s: String = null
    val sb: StringBuilder = new StringBuilder
    while ((({
      s = reader.readLine;
      s
    })) != null) {
      sb.append(s + "\n")
    }
    parseXMLResponse(sb.toString())
    //    httpConn.disconnect()
    this
  }

  private def buildXMLRequest: String = {
    val xml: StringBuilder = new StringBuilder
    if (requestType == "") {
      throw new Exception
    }
    xml.append("<" + requestType + ">\n")
    for (field <- requestParams.keySet().toArray) {
      xml.append("<" + field.toString + ">" + requestParams.get(field.toString) + "</" + field.toString + ">\n")
    }
    xml.append("</" + requestType + ">\n")
    xml.toString()
  }


  private def parseXMLResponse(xml: String) {
    val factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val docBuilder: DocumentBuilder = factory.newDocumentBuilder
    val doc: Document = docBuilder.parse(new InputSource(new StringReader(xml)))
    responseType = doc.getDocumentElement.getTagName
    val docNode: Node = doc.getFirstChild
    for (i <- 0 until docNode.getChildNodes.getLength) {
      val node = docNode.getChildNodes.item(i)
      if (node.getNodeType == Node.ELEMENT_NODE) {
        responseValues.put(node.getNodeName, node.getTextContent)
      }
    }
  }

  def setParameter(paramName: String, paramValue: String) {
    if (paramName != null && paramValue != null) {
      requestParams.put(paramName, paramValue)
    }
  }

  def setRequestType(requestType: String) {
    if (requestType != null) {
      this.requestType = requestType
    }
  }

  def getResponseValue(valueName: String): String = {
    responseValues.get(valueName)
  }

  def getResponseType: String = {
    responseType
  }
}


object VBGBiometricServices {
  val _url: URL = new URL("https://service03.voicebiogroup.com/service/xmlapi")
  private val _myClientName: String = "celebritydev"
  private val _myClientKey: String = "62ed7855e0af30d0af534ce195845c7f"
  private val _StartEnrollment: String = "StartEnrollment"
  private val _FinishTransaction: String = "FinishTransaction"
  private val _AudioCheck: String = "AudioCheck"
  private val _EnrollUser: String = "EnrollUser"
  private val _StartVerification: String = "StartVerification"
  private val _VerifySample: String = "VerifySample"
  //    private val _RenameUser: String = "RenameUser";
  //    private val _CheckUserStatus: String  = "CheckUserStatus";
  //    private val _SetUserStatus: String  = "SetUserStatus"; // {“active”, “inactive”, “locked”, “opted-out”, “deleted”}

  // VBG requires that these Strings be lower-cased.
  private val _clientKey: String = "clientkey"
  private val _clientName: String = "clientname"
  val _errorCode: String = "errorcode"
  private val _prompt: String = "prompt"
  private val _rebuildTemplate: String = "rebuildtemplate"
  val _score: String = "score"
  val _success: String = "success"
  val _transactionId: String = "transactionid"
  private val _voiceSample: String = "voicesample"
  val _usableTime: String = "usabletime"
  private val _userId: String = "userid"

  def sendStartEnrollmentRequest(userId: String, rebuildTemplate: Boolean): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_StartEnrollment)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_userId, userId)
    request.setParameter(_rebuildTemplate, rebuildTemplate.toString)
    request.sendRequest()
  }

  def sendAudioCheckRequest(transactionId: String, blobLocation: String): VBGRequest = {
    val request = new VBGRequest
    val voiceSampleBase64_downSampled: String = convertWavTo8kHzBase64(Blobs.get(blobLocation).get.asByteArray)
    request.setRequestType(_AudioCheck)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_transactionId, transactionId)
    request.setParameter(_voiceSample, voiceSampleBase64_downSampled)
    request.sendRequest()
  }

  def sendEnrollUserRequest(transactionId: String): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_EnrollUser)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_transactionId, transactionId)
    request.sendRequest()
  }

  def sendFinishEnrollTransactionRequest(transactionId: String, successValue: String): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_FinishTransaction)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_transactionId, transactionId)
    request.setParameter(_success, successValue)
    request.sendRequest()
  }

  def sendFinishVerifyTransactionRequest(transactionId: String, successValue: String, score: String): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_FinishTransaction)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_transactionId, transactionId)
    request.setParameter(_success, successValue)
    request.setParameter(_score, score)
    request.sendRequest()
  }

  def sendStartVerificationRequest(userId: String): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_StartVerification)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_userId, userId)
    request.sendRequest()
  }

  def sendVerifySampleRequest(transactionId: String, wavBinary: Array[Byte]): VBGRequest = {
    val voiceSampleBase64_downSampled: String = convertWavTo8kHzBase64(wavBinary)

    val request = new VBGRequest
    request.setRequestType(_VerifySample)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_transactionId, transactionId)
    request.setParameter(_voiceSample, voiceSampleBase64_downSampled)
    request.sendRequest()
  }

  // ========================== HELPERS

  // Depends on iPad issue 49.
  def convertWavTo8kHzBase64(wavBinary: Array[Byte]): String = {
    val wavBinary_8kHz: Array[Byte] = SampleRateConverter.convert(8000f, wavBinary)
    Codec.encodeBASE64(wavBinary_8kHz)
  }
}
