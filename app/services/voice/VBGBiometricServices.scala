package services.voice

import java.util.Hashtable
import java.net.URL
import org.apache.commons.codec.binary.Base64
import javax.xml.parsers.{DocumentBuilder, DocumentBuilderFactory}
import org.xml.sax.InputSource
import java.io.{StringReader, FileInputStream, ByteArrayOutputStream}
import org.w3c.dom.{NodeList, Node, Document}

class VBGRequest {
  private var requesttype: String = ""
  private var responsetype: String = ""
  private var requestparams: Hashtable[String, String] = new Hashtable[String, String]
  private var responsevalues: Hashtable[String, String] = new Hashtable[String, String]


  def sendRequest = {

  }

  def setParameter(paramname: String, paramvalue: String): Unit = {
    if (paramname != null && paramvalue != null) {
      requestparams.put(paramname, paramvalue)
    }
  }

  def setRequestType(requesttype: String): Unit = {
    if (requesttype != null) {
      this.requesttype = requesttype
    }
  }

  def getResponseValue(valuename: String): String = {
    return responsevalues.get(valuename)
  }

  def getResponseType: String = {
    return responsetype
  }

  private def buildXMLRequest: String = {
    val xml: StringBuilder = new StringBuilder
    var field: String = null
    if (requesttype == "") {
      throw new Exception
    }
    xml.append("<" + requesttype + ">\n")
//    val fields: Enumeration[String] = requestparams.keys
//    while (fields.hasMoreElements) {
//      field = fields.nextElement
//      xml.append("<" + field + ">" + requestparams.get(field) + "</" + field + ">\n")
//    }
//    xml.append("</" + requesttype + ">\n")
    xml.toString
  }


  private def parseXMLResponse(xml: String): Unit = {
    var factory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    var docbuilder: DocumentBuilder = factory.newDocumentBuilder
    var doc: Document = docbuilder.parse(new InputSource(new StringReader(xml)))
    var docType: String = doc.getDocumentElement.getTagName
    var node: Node = doc.getFirstChild
    var nodes: NodeList = node.getChildNodes
    responsetype = docType
    //        for (int i = 0; i < nodes.getLength(); i++) {
    //            node = nodes.item(i);
    //            if (node.getNodeType() == Node.ELEMENT_NODE) {
    //                responsevalues.put(node.getNodeName(), node.getTextContent());
    //            }
    //        }
    return
  }

}


object VBGBiometricServices {
  private val _url: URL = new URL("https://service03.voicebiogroup.com/service/xmlapi")
  private val _myClientname: String = "celebritydev"
  private val _myClientkey: String = "62ed7855e0af30d0af534ce195845c7f"
  private val _StartEnrollment: String = "StartEnrollment"
  private val _FinishTransaction: String = "FinishTransaction"
  private val _AudioCheck: String = "AudioCheck"
  private val _EnrollUser: String = "EnrollUser"
  private val _StartVerification: String = "StartVerification"
  private val _VerifySample: String = "VerifySample"
  //    private val _RenameUser: String = "RenameUser";
  //    private val _CheckUserStatus: String  = "CheckUserStatus";
  //    private val _SetUserStatus: String  = "SetUserStatus"; // {“active”, “inactive”, “locked”, “opted-out”, “deleted”}
  private val _clientkey: String = "clientkey"
  private val _clientname: String = "clientname"
  private val _errorcode: String = "errorcode"
  private val _prompt: String = "prompt"
  private val _rebuildtemplate: String = "rebuildtemplate"
  private val _score: String = "score"
  private val _success: String = "success"
  private val _transactionid: String = "transactionid"
  private val _voicesample: String = "voicesample"
  private val _usabletime: String = "usabletime"
  private val _userid: String = "userid"

  def sendStartEnrollmentRequest(userid: String, rebuildtemplate: Boolean): Unit = {
    val request = new VBGRequest
    request.setRequestType(_StartEnrollment)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_userid, userid)
    request.setParameter(_rebuildtemplate, rebuildtemplate.toString)
    request.sendRequest
  }

  def sendAudioCheckRequest(transactionId: String, filename: String): Unit = {
    val request = new VBGRequest
    val voicesample: String = getVoicesampleBase64Encoded(filename)
    request.setRequestType(_AudioCheck)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_transactionid, transactionId)
    request.setParameter(_voicesample, voicesample)
    request.sendRequest
  }

  def sendEnrollUserRequest(transactionId: String): Unit = {
    val request = new VBGRequest
    request.setRequestType(_EnrollUser)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_transactionid, transactionId)
    request.sendRequest
  }

  def sendFinishEnrollTransactionRequest(transactionId: String, successValue: String): Unit = {
    val request = new VBGRequest
    request.setRequestType(_FinishTransaction)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_transactionid, transactionId)
    request.setParameter(_success, successValue)
    request.sendRequest
  }

  def sendFinishVerifyTransactionRequest(transactionId: String, successValue: String, score: String): Unit = {
    val request = new VBGRequest
    request.setRequestType(_FinishTransaction)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_transactionid, transactionId)
    request.setParameter(_success, successValue)
    request.setParameter(_score, score)
    request.sendRequest
  }

  def sendStartVerificationRequest(userid: String): Unit = {
    val request = new VBGRequest
    request.setRequestType(_StartVerification)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_userid, userid)
    request.sendRequest
  }

  def sendVerifySampleRequest(transactionId: String, filename: String): Unit = {
    val request = new VBGRequest
    val voicesample: String = getVoicesampleBase64Encoded(filename)
    request.setRequestType(_VerifySample)
    request.setParameter(_clientname, _myClientname)
    request.setParameter(_clientkey, _myClientkey)
    request.setParameter(_transactionid, transactionId)
    request.setParameter(_voicesample, voicesample)
    request.sendRequest
  }


  // ========================== PRIVATE HELPERS

  private def getVoicesampleBase64Encoded(filename: String): String = {
    val bas: ByteArrayOutputStream = new ByteArrayOutputStream
    var fdata: Int = 0
    val fs: FileInputStream = new FileInputStream(filename)
    while (fs.available > 0) {
      fdata = fs.read
      bas.write(fdata)
    }
    fs.close
    Base64.encodeBase64String(bas.toByteArray)
  }


}