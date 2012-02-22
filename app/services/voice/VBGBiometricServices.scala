package services.voice

import java.io._
import java.io.{ByteArrayInputStream, SequenceInputStream}
import java.net.{URLEncoder, URL}
import java.util.Hashtable
import javax.net.ssl.HttpsURLConnection
import javax.sound.sampled.{AudioInputStream, AudioSystem}
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import org.w3c.dom.{Node, Document}
import org.xml.sax.InputSource
import play.libs.Codec
import services.{AppConfig, SampleRateConverter}
import services.blobs.Blobs
import services.blobs.Blobs.Conversions._

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

    println("VBGBiometricServices httpConn = " + httpConn.toString)

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
  private val _clientKey = "clientkey"
  private val _clientName = "clientname"
  val _errorCode = "errorcode"
  val _prompt = "prompt"
  private val _rebuildTemplate = "rebuildtemplate"
  val _score = "score"
  val _success = "success"
  val _transactionId = "transactionid"
  private val _voiceSample = "voicesample"
  val _usableTime = "usabletime"
  private val _userId = "userid"
  private val blobs = AppConfig.instance[Blobs]

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
    val voiceSampleBase64_downSampled: String = convertWavTo8kHzBase64(blobs.get(blobLocation).get.asByteArray)
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

  def requestFinishVerifyTransaction(transactionId: String, successValue: String, score: String) {
    val request = sendFinishVerifyTransactionRequest(transactionId, successValue, score)
    val code = request.getResponseValue(_errorCode)
    if (VoiceBiometricsCode.byCodeString(code) != VoiceBiometricsCode.Success) {
      println(
        "Failed to close VerifySample transaction " + transactionId + ", though that shouldn't" +
        " matter. Here's the stack trace anyways."
      )
      VoiceBiometricsError(code, request).printStackTrace()
    }
  }

  def sendStartVerificationRequest(userId: String): VBGRequest = {
    val request = new VBGRequest
    request.setRequestType(_StartVerification)
    request.setParameter(_clientName, _myClientName)
    request.setParameter(_clientKey, _myClientKey)
    request.setParameter(_userId, userId)
    request.sendRequest()
  }

  def requestStartVerification(userId: String): Either[VoiceBiometricsError, StartVerificationResponse] = {
    withSuccessfulRequest(sendStartVerificationRequest(userId)) { request =>
      new StartVerificationResponse(request)
    }
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

  def requestVerifySample(transactionId: String, wavBinary: Array[Byte]): Either[VoiceBiometricsError, VerifySampleResponse] = {
    withSuccessfulRequest(sendStartVerificationRequest(transactionId)) { request =>
      new VerifySampleResponse(request)
    }
  }

  // ========================== HELPERS

  // Depends on iPad issue 49.
  def convertWavTo8kHzBase64(wavBinary: Array[Byte]): String = {
    val wavBinary_8kHz: Array[Byte] = SampleRateConverter.convert(8000f, wavBinary)
    Codec.encodeBASE64(wavBinary_8kHz)
  }

  // Referenced http://stackoverflow.com/questions/6381012/java-trouble-combining-more-than-2-wav-files
  def stitchWAVs(listOfWavBinaries: List[Array[Byte]]): Option[AudioInputStream] = {
    if (listOfWavBinaries.length > 1) {
      val audio0: AudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(listOfWavBinaries.head))
      val audio1: AudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(listOfWavBinaries.tail.head))
      var audioBuilder: AudioInputStream = new AudioInputStream(
        new SequenceInputStream(audio0, audio1),
        audio0.getFormat,
        audio0.getFrameLength + audio1.getFrameLength)

      for (wavBinary <- listOfWavBinaries.tail.tail) {
        val currAudio: AudioInputStream = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wavBinary))
        audioBuilder = new AudioInputStream(
          new SequenceInputStream(audioBuilder, currAudio),
          audioBuilder.getFormat,
          audioBuilder.getFrameLength + currAudio.getFrameLength)
      }
      Some(audioBuilder)

    } else if (listOfWavBinaries.length == 1) {
      Some(AudioSystem.getAudioInputStream(new ByteArrayInputStream(listOfWavBinaries.head)))

    } else {
      None
    }
  }

  /**
   * Filters out unsuccessful VBG service requests.
   *
   * Returns the value of the `continue` on the Right if the `request`'s response code was
   * [[services.voice.VoiceBiometricsCode.Success]]. If the response wasn't successful but the code was found in
   * [[services.voice.VoiceBiometricsCode]], it means some code in our codebase knows how to handle the code and
   * returns a [[services.voice.VoiceBiometricsError]] on the Left. If the code was not located in that enum, it throws
   * the VoiceBiometricsError rather than returning it
   */
  private def withSuccessfulRequest[T](request: VBGRequest)(continue: (VBGRequest) => T): Either[VoiceBiometricsError, T] = {
    import VoiceBiometricsCode._

    val errorCodeString = request.getResponseValue(_errorCode)

    VoiceBiometricsCode.byCodeString.get(errorCodeString) match {
      // Request was groovy!
      case Some(Success) =>
        Right(continue(request))

      // Things could've gone better, but at least someone in our codebase knows how to handle the error
      case Some(errorCode) =>
        Left(VoiceBiometricsError(errorCode, request))

      // Nobody knows how to handle this error code. Throw up _everywhere_!
      case None =>
        throw VoiceBiometricsError(errorCodeString, request)
    }
  }
}

class StartVerificationResponse(request: VBGRequest) {
  lazy val transactionId = request.getResponseValue(VBGBiometricServices._transactionId)
  lazy val prompt = request.getResponseValue(VBGBiometricServices._prompt)
}

class VerifySampleResponse(request: VBGRequest) extends VoiceVerificationResult {
  override lazy val score = request.getResponseValue(VBGBiometricServices._score).toInt
  override lazy val success = request.getResponseValue(VBGBiometricServices._success).toBoolean
  lazy val usableTime = request.getResponseValue(VBGBiometricServices._usableTime).toDouble

  lazy val prompt = request.getResponseValue(VBGBiometricServices._prompt)
}