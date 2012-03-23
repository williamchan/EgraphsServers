package services.voice

import java.io._
import java.io.{ByteArrayInputStream, SequenceInputStream}
import java.net.{URLEncoder, URL}
import java.util.Hashtable
import javax.net.ssl.HttpsURLConnection
import javax.sound.sampled.{AudioInputStream, AudioSystem}
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import models._
import models.vbg._
import org.w3c.dom.{Node, Document}
import org.xml.sax.InputSource
import play.libs.Codec
import services.SampleRateConverter

trait VBGBiometricServicesBase {

  protected val _url: URL
  protected val _myClientName: String
  protected val _myClientKey: String
  protected val _userIdPrefix: String

  def enroll(enrollmentBatch: EnrollmentBatch): Either[VoiceBiometricsError, Boolean] = {
    val vbgStartEnrollment: VBGStartEnrollment = startEnrollment(enrollmentBatch)
    vbgStartEnrollment.save()
    val startEnrollmentError: Option[VoiceBiometricsError] = maybeGetVoiceBiometricsError(vbgStartEnrollment)
    if (startEnrollmentError.isDefined) {
      return Left(startEnrollmentError.get)
    }

    val enrollmentSamples: List[EnrollmentSample] = enrollmentBatch.getEnrollmentSamples
    val wavs: List[Array[Byte]] = for (enrollmentSample <- enrollmentSamples) yield enrollmentSample.getWav

    // this part differs between RandomNumber and FreeSpeech...
    //    for (voiceSample <- voiceSamples) {
    //      val vbgAudioCheck = sendAudioCheckRequest(enrollmentBatch, transactionId, VoiceSample.getWavUrl(voiceSample.id))
    //    }

    val transactionId = vbgStartEnrollment.vbgTransactionId.get
    val combinedWav: Option[AudioInputStream] = stitchWAVs(wavs)
    val combinedWavBinary: Array[Byte] = if (combinedWav.isDefined) convertAudioInputStreamToByteArray(combinedWav.get) else new Array[Byte](0)
    val vbgAudioCheck: VBGAudioCheck = sendAudioCheckRequest(enrollmentBatch = enrollmentBatch, transactionId = transactionId, wavBinary = combinedWavBinary)
    vbgAudioCheck.save()
    saveCombinedWavToBlobStore(enrollmentBatch, combinedWavBinary)
    val audioCheckError: Option[VoiceBiometricsError] = maybeGetVoiceBiometricsError(vbgAudioCheck)
    if (audioCheckError.isDefined) {
      return Left(audioCheckError.get)
    }

    val vbgEnrollUser: VBGEnrollUser = sendEnrollUserRequest(enrollmentBatch, transactionId)
    vbgEnrollUser.save()
    val enrollUserError: Option[VoiceBiometricsError] = maybeGetVoiceBiometricsError(vbgEnrollUser)
    if (enrollUserError.isDefined) {
      return Left(enrollUserError.get)
    }

    val enrollmentSuccessValue = vbgEnrollUser.success.get
    val vbgFinishEnrollTransaction: VBGFinishEnrollTransaction = sendFinishEnrollTransactionRequest(enrollmentBatch, transactionId, enrollmentSuccessValue)
    vbgFinishEnrollTransaction.save()
    Right(enrollmentSuccessValue)
  }

  /**
   *
   * @param egraph Egraph to biometrically verify
   * @return verification result
   */
  def verify(egraph: Egraph): Either[VoiceBiometricsError, VBGVerifySample] = {
    val vbgStartVerification: VBGStartVerification = sendStartVerificationRequest(egraph)
    vbgStartVerification.save()
    val startVerificationError: Option[VoiceBiometricsError] = maybeGetVoiceBiometricsError(vbgStartVerification)
    if (startVerificationError.isDefined) {
      return Left(startVerificationError.get)
    }

    val transactionId: Long = vbgStartVerification.vbgTransactionId.get
    val vbgVerifySample: VBGVerifySample = sendVerifySampleRequest(egraph, transactionId)
    vbgVerifySample.save()
    var success = false
    var score: Long = 0
    val verifySampleError: Option[VoiceBiometricsError] = maybeGetVoiceBiometricsError(vbgVerifySample)
    if (verifySampleError.isDefined) {
      val vbgFinishVerityTransaction: VBGFinishVerifyTransaction = sendFinishVerifyTransactionRequest(egraph, transactionId = transactionId, successValue = success, score = score)
      vbgFinishVerityTransaction.save()
      Left(verifySampleError.get)
    } else {
      // if either of these are ever undefined, then VBG's API is messed up and we should throw an error.
      success = vbgVerifySample.success.get
      score = vbgVerifySample.score.get
      val vbgFinishVerityTransaction: VBGFinishVerifyTransaction = sendFinishVerifyTransactionRequest(egraph, transactionId = transactionId, successValue = success, score = score)
      vbgFinishVerityTransaction.save()
      Right(vbgVerifySample)
    }
  }


  // ========================== make API calls

  protected[voice] def startEnrollment(enrollmentBatch: EnrollmentBatch): VBGStartEnrollment = {
    val vbgStartEnrollmentFirstAttempt = sendStartEnrollmentRequest(enrollmentBatch = enrollmentBatch, rebuildTemplate = false)
    if (VoiceBiometricsCode.Success == VoiceBiometricsCode.byCodeString(vbgStartEnrollmentFirstAttempt.errorCode)) {
      // First-time enrollment
      vbgStartEnrollmentFirstAttempt
    } else {
      vbgStartEnrollmentFirstAttempt.save()
      // Re-enrollment
      sendStartEnrollmentRequest(enrollmentBatch = enrollmentBatch, rebuildTemplate = true)
    }
  }

  protected[voice] def sendStartEnrollmentRequest(enrollmentBatch: EnrollmentBatch, rebuildTemplate: Boolean): VBGStartEnrollment = {
    val userId = getUserId(celebrityId = enrollmentBatch.celebrityId)

    val request = new VBGRequest
    request.setRequestType(VBGRequest._StartEnrollment)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._userId, userId)
    request.setParameter(VBGRequest._rebuildTemplate, rebuildTemplate.toString)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val rawTransactionId: String = request.getResponseValue(VBGRequest._transactionId)
    val vbgTransactionId: Option[Long] = if (rawTransactionId != null) Some(rawTransactionId.toLong) else None
    val vbgStartEnrollment = new VBGStartEnrollment(enrollmentBatchId = enrollmentBatch.id, errorCode = errorCode, vbgTransactionId = vbgTransactionId)

    vbgStartEnrollment
  }

  protected[voice] def sendAudioCheckRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, wavBinary: Array[Byte]): VBGAudioCheck = {
    val wav8kHzBase64: String = convertWavTo8kHzBase64(wavBinary)

    val request = new VBGRequest
    request.setRequestType(VBGRequest._AudioCheck)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._transactionId, transactionId.toString)
    request.setParameter(VBGRequest._voiceSample, wav8kHzBase64)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val rawUsableTime: String = request.getResponseValue(VBGRequest._usableTime)
    val usableTime: Option[Double] = if (rawUsableTime != null) Some(rawUsableTime.toDouble) else None
    val vbgAudioCheck = new VBGAudioCheck(enrollmentBatchId = enrollmentBatch.id, errorCode = errorCode, vbgTransactionId = transactionId, usableTime = usableTime)

    vbgAudioCheck
  }

  protected[voice] def sendEnrollUserRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long): VBGEnrollUser = {
    val request = new VBGRequest
    request.setRequestType(VBGRequest._EnrollUser)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._transactionId, transactionId.toString)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val rawSuccessValue: String = request.getResponseValue(VBGRequest._success)
    val success: Option[Boolean] = if (rawSuccessValue != null) Some(rawSuccessValue.toBoolean) else None
    val vbgEnrollUser = new VBGEnrollUser(enrollmentBatchId = enrollmentBatch.id, errorCode = errorCode, vbgTransactionId = transactionId, success = success)

    vbgEnrollUser
  }

  protected[voice] def sendFinishEnrollTransactionRequest(enrollmentBatch: EnrollmentBatch, transactionId: Long, successValue: Boolean): VBGFinishEnrollTransaction = {
    val request = new VBGRequest
    request.setRequestType(VBGRequest._FinishTransaction)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._transactionId, transactionId.toString)
    request.setParameter(VBGRequest._success, successValue.toString)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val vbgFinishEnrollTransaction = new VBGFinishEnrollTransaction(enrollmentBatchId = enrollmentBatch.id, errorCode = errorCode, vbgTransactionId = transactionId)

    vbgFinishEnrollTransaction
  }

  protected[voice] def sendStartVerificationRequest(egraph: Egraph): VBGStartVerification = {
    val userId = getUserId(celebrityId = egraph.celebrity.id)

    val request = new VBGRequest
    request.setRequestType(VBGRequest._StartVerification)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._userId, userId)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val rawTransactionId: String = request.getResponseValue(VBGRequest._transactionId)
    val vbgTransactionId: Option[Long] = if (rawTransactionId != null) Some(rawTransactionId.toLong) else None
    val vbgStartVerification = new VBGStartVerification(egraphId = egraph.id, errorCode = errorCode, vbgTransactionId = vbgTransactionId)

    vbgStartVerification
  }

  protected[voice] def sendVerifySampleRequest(egraph: Egraph, transactionId: Long): VBGVerifySample = {
    import services.blobs.Blobs.Conversions._
    val wavBinary: Array[Byte] = egraph.assets.audio.asByteArray
    val voiceSampleBase64_downSampled: String = convertWavTo8kHzBase64(wavBinary)

    val request = new VBGRequest
    request.setRequestType(VBGRequest._VerifySample)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._transactionId, transactionId.toString)
    request.setParameter(VBGRequest._voiceSample, voiceSampleBase64_downSampled)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val rawSuccessValue: String = request.getResponseValue(VBGRequest._success)
    val success: Option[Boolean] = if (rawSuccessValue != null) Some(rawSuccessValue.toBoolean) else None
    val rawScoreValue: String = request.getResponseValue(VBGRequest._score)
    val score: Option[Long] = if (rawSuccessValue != null) Some(rawScoreValue.toLong) else None
    val rawUsableTime: String = request.getResponseValue(VBGRequest._usableTime)
    val usableTime: Option[Double] = if (rawUsableTime != null) Some(rawUsableTime.toDouble) else None
    val vbgVerifySample = new VBGVerifySample(egraphId = egraph.id, errorCode = errorCode, vbgTransactionId = transactionId, score = score, success = success, usableTime = usableTime)

    vbgVerifySample
  }

  protected[voice] def sendFinishVerifyTransactionRequest(egraph: Egraph, transactionId: Long, successValue: Boolean, score: Long): VBGFinishVerifyTransaction = {
    val request = new VBGRequest
    request.setRequestType(VBGRequest._FinishTransaction)
    request.setParameter(VBGRequest._clientName, _myClientName)
    request.setParameter(VBGRequest._clientKey, _myClientKey)
    request.setParameter(VBGRequest._transactionId, transactionId.toString)
    request.setParameter(VBGRequest._success, successValue.toString)
    request.setParameter(VBGRequest._score, score.toString)
    request.sendRequest(_url)

    val errorCode: String = request.getResponseValue(VBGRequest._errorCode)
    val vbgFinishVerifyTransaction = new VBGFinishVerifyTransaction(egraphId = egraph.id, errorCode = errorCode, vbgTransactionId = transactionId)

    vbgFinishVerifyTransaction
  }

  final protected[voice] def getUserId(celebrityId: Long): String = {
    _userIdPrefix + celebrityId.toString
  }

  //  def requestStartVerification(userId: String): Either[VoiceBiometricsError, StartVerificationResponse] = {
  //    withSuccessfulRequest(sendStartVerificationRequest(userId)) {
  //      vbgBase =>
  //        new StartVerificationResponse(vbgBase)
  //    }
  //  }
  //
  //  def requestVerifySample(transactionId: Long, wavBinary: Array[Byte]): Either[VoiceBiometricsError, VerifySampleResponse] = {
  //    withSuccessfulRequest(sendVerifySampleRequest(transactionId, wavBinary)) {
  //      vbgBase =>
  //        new VerifySampleResponse(vbgBase)
  //    }
  //  }
  //
  //  def requestFinishVerifyTransaction(transactionId: Long, successValue: Boolean, score: Long) {
  //    val vbgBase = sendFinishVerifyTransactionRequest(transactionId, successValue, score)
  //    val code = vbgBase.errorCode
  //    if (VoiceBiometricsCode.byCodeString(code) != VoiceBiometricsCode.Success) {
  //      println(
  //        "Failed to close VerifySample transaction " + transactionId + ", though that shouldn't" +
  //          " matter. Here's the stack trace anyways."
  //      )
  //      VoiceBiometricsError(code, vbgBase).printStackTrace()
  //    }
  //  }

  // ========================== HELPERS

  // Depends on iPad issue 49.
  final protected[voice] def convertWavTo8kHzBase64(wavBinary: Array[Byte]): String = {
    if (wavBinary.length == 0) return ""
    val wavBinary_8kHz: Array[Byte] = SampleRateConverter.convert(8000f, wavBinary)
    Codec.encodeBASE64(wavBinary_8kHz)
  }

  // Referenced http://stackoverflow.com/questions/6381012/java-trouble-combining-more-than-2-wav-files
  final protected[voice] def stitchWAVs(listOfWavBinaries: List[Array[Byte]]): Option[AudioInputStream] = {
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

  final protected[voice] def convertAudioInputStreamToByteArray(audioInputStream: AudioInputStream): Array[Byte] = {
    val bas: ByteArrayOutputStream = new ByteArrayOutputStream()
    AudioSystem.write(audioInputStream, javax.sound.sampled.AudioFileFormat.Type.WAVE, bas)
    bas.toByteArray
  }

  /**
   * Saving these combined WAvs to the blobstore is useful in case we want to listen to them when reviewing enrollments.
   */
  private def saveCombinedWavToBlobStore(enrollmentBatch: EnrollmentBatch, combinedWavBinary: Array[Byte]) {
    enrollmentBatch.services.blobs.put(EnrollmentBatch.getCombinedWavUrl(enrollmentBatch.id), combinedWavBinary)
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
  //  private def withSuccessfulRequest[T](request: VBGRequest)(continue: (VBGRequest) => T): Either[VoiceBiometricsError, T] = {
  //    import VoiceBiometricsCode._
  //
  //    val errorCodeString = request.errorCode
  //
  //    VoiceBiometricsCode.byCodeString.get(errorCodeString) match {
  //      // Request was groovy!
  //      case Some(Success) =>
  //        Right(continue(request))
  //
  //      // Things could've gone better, but at least someone in our codebase knows how to handle the error
  //      case Some(errorCode) =>
  //        Left(VoiceBiometricsError(errorCode, request))
  //
  //      // Nobody knows how to handle this error code. Throw up _everywhere_!
  //      case None =>
  //        throw VoiceBiometricsError(errorCodeString, request)
  //    }
  //  }

  private def maybeGetVoiceBiometricsError(vbgBase: VBGBase): Option[VoiceBiometricsError] = {
    import VoiceBiometricsCode._
    val errorCodeString = vbgBase.getErrorCode
    VoiceBiometricsCode.byCodeString.get(errorCodeString) match {
      // Request was groovy!
      case Some(Success) =>
        None

      // Things could've gone better, but at least someone in our codebase knows how to handle the error
      case Some(errorCode) =>
        Some(VoiceBiometricsError(errorCode, vbgBase))

      // Nobody knows how to handle this error code. Throw up _everywhere_!
      case None =>
        throw VoiceBiometricsError(errorCodeString, vbgBase)
    }
  }
}

object VBGRequest {
  val _StartEnrollment: String = "StartEnrollment"
  val _FinishTransaction: String = "FinishTransaction"
  val _AudioCheck: String = "AudioCheck"
  val _EnrollUser: String = "EnrollUser"
  val _StartVerification: String = "StartVerification"
  val _VerifySample: String = "VerifySample"
  //    private val _RenameUser: String = "RenameUser";
  //    private val _CheckUserStatus: String  = "CheckUserStatus";
  //    private val _SetUserStatus: String  = "SetUserStatus"; // {“active”, “inactive”, “locked”, “opted-out”, “deleted”}

  val _clientKey = "clientkey"
  val _clientName = "clientname"
  val _errorCode = "errorcode"
  //  val _prompt = "prompt"
  val _rebuildTemplate = "rebuildtemplate"
  val _score = "score"
  val _success = "success"
  val _transactionId = "transactionid"
  val _voiceSample = "voicesample"
  val _usableTime = "usabletime"
  val _userId = "userid"
}

private class VBGRequest {
  private var requestType: String = ""
  private var responseType: String = ""
  private val requestParams: Hashtable[String, String] = new Hashtable[String, String]
  private val responseValues: Hashtable[String, String] = new Hashtable[String, String]

  def sendRequest(url: URL): VBGRequest = {
    // Sends vbgBase using internal parameters and sets internal return values
    // Accepts a field name to use for the message (can be null)

    var xml: String = buildXMLRequest
    // Make sure to encode the output stream for any weird characters
    xml = URLEncoder.encode(xml, "UTF-8")
    val httpConn: HttpsURLConnection = url.openConnection.asInstanceOf[HttpsURLConnection]
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