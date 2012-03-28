package services.http

import utils.EgraphsUnitTest
import play.mvc.Http.Request
import play.libs.Crypto
import java.util.Date

class RequestInfoTest extends EgraphsUnitTest {
  "RequestInfo" should "return the first few letters of the IP address's MD5 hash for client ID" in {
    val request = mock[Request]

    request.remoteAddress = "herp"

    val underTest = new RequestInfo(request)

    underTest.clientId should be (Crypto.passwordHash("herp", Crypto.HashType.MD5).substring(0, 6))
  }

  it should "generate the request ID from the IP address, url, and request time, and the clientId only from the IP" in {
    val now = new Date()
    
    val firstInfo = requestInfo("1.1.1.1", "/home", now)
    val sameInfo = requestInfo("1.1.1.1", "/home", now)
    val differentAddress = requestInfo("2.1.1.1", "/home", now)
    val differentUrl = requestInfo("1.1.1.1", "/elsewhere", now)
    val differentDate = requestInfo("1.1.1.1", "/home", new Date(1924919L))

    firstInfo.requestId should be (sameInfo.requestId)
    firstInfo.requestId should not be (differentAddress.requestId)
    firstInfo.requestId should not be (differentUrl.requestId)
    firstInfo.requestId should not be (differentDate.requestId)

    firstInfo.clientId should be (sameInfo.clientId)
    firstInfo.clientId should be (differentUrl.clientId)
    firstInfo.clientId should be (differentDate.clientId)
    firstInfo.clientId should not be (differentAddress.clientId)
  }
  
  def requestInfo(remoteAddress: String, url: String, date: Date) = {
    val request = mock[Request]
    
    request.remoteAddress = remoteAddress
    request.url = url
    request.date = date
    
    new RequestInfo(request)
  }
}
