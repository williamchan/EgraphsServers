package services

import utils._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.config.ConfigFileProxy

@RunWith(classOf[JUnitRunner])
class ConsumerApplicationTests extends EgraphsUnitTest {
  
  "get" should "prepend the relative url with the configured base url" in {
    val config = mock[ConfigFileProxy]
    config.applicationBaseUrl returns "https://www.egraphs.com/"
    new ConsumerApplication(config).absoluteUrl("/Wizzle/photos/2010-Starcraft-2-Championships") should be("https://www.egraphs.com/Wizzle/photos/2010-Starcraft-2-Championships")
  }

  "compose" should "include just one slash when joining a base URL to a relative URL" in {
    val consumerApp = new ConsumerApplication(mock[ConfigFileProxy])
    consumerApp.compose("https://www.egraphs.com/", "/login") should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com/", "login")  should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com" , "/login") should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com" , "login")  should be("https://www.egraphs.com/login")
  }
}