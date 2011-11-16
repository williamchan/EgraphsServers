package libs

import xml.{Null, UnprefixedAttribute, Node, Elem}
import org.apache.batik.transcoder.image.PNGTranscoder
import org.w3c.dom.Document
import org.apache.batik.transcoder.{TranscoderOutput, TranscoderInput}
import java.io.{InputStream, ByteArrayInputStream, FileOutputStream}
import javax.xml.parsers.DocumentBuilderFactory

object ImageUtil {

    val defaultStyle = "fill:none;fill-opacity:0.75;fill-rule:evenodd;stroke:rgb(0,0,0);stroke-width:1.4;stroke-linecap:round;stroke-linejoin:round;stroke-opacity:1;stroke-miterlimit:4;stroke-dasharray:none"
//  val defaultStyle = "fill:none;fill-opacity:1.00;fill-rule:evenodd;stroke:rgb(0,0,0);stroke-width:3.0;stroke-linecap:round;stroke-linejoin:round;stroke-opacity:1;stroke-miterlimit:4;stroke-dasharray:none"

  def createSVG(pathNodes: List[Elem]): Elem = {
    var xml = <svg xmlns="http://www.w3.org/2000/svg" version="1.1" width="1024" height="768"></svg>
    for (pathNode <- pathNodes) {
      xml = addChild(xml, pathNode)
    }
    xml
  }

  def addChild(n: Node, newChild: Node) = n match {
    case Elem(prefix, label, attributes, scope, child@_*) =>
      Elem(prefix, label, attributes, scope, child ++ newChild: _*)
    case _ => error("Can only add children to elements!")
  }

  def createPathNode(d: String, style: String): Elem = {
    val res = <path/> % new UnprefixedAttribute("style", style, Null) % new UnprefixedAttribute("d", d, Null)
    res
  }

  def saveAsPNG(svgXML: String, file: String) {
    val t = new PNGTranscoder()
    val document: Document = loadXMLFrom(svgXML)
    val input = new TranscoderInput(document)
    val ostream = new FileOutputStream(file)
    val output = new TranscoderOutput(ostream)
    t.transcode(input, output)
    ostream.flush()
    ostream.close()
  }

  def loadXMLFrom(str: String): Document = {
    loadXMLFrom(new ByteArrayInputStream(str.getBytes))
  }

  def loadXMLFrom(is: InputStream): Document = {
    val factory = DocumentBuilderFactory.newInstance()
    factory.setNamespaceAware(true)
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(is)
    is.close()
    doc
  }

}