package services

import utils._
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import services.config.ConfigFileProxy

@RunWith(classOf[JUnitRunner])
class ConsumerApplicationTests extends EgraphsUnitTest {
  
  "get" should "prepend the relative url with the configured base url" in {
    val (consumerApp, _) = newConsumerApp()
    
    consumerApp.absoluteUrl("/Wizzle/photos/2010-Starcraft-2-Championships") should be("https://www.egraphs.com/Wizzle/photos/2010-Starcraft-2-Championships")
  }

  "compose" should "include just one slash when joining a base URL to a relative URL" in {
    val (consumerApp, _) = newConsumerApp()
    consumerApp.compose("https://www.egraphs.com/", "/login") should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com/", "login")  should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com" , "/login") should be("https://www.egraphs.com/login")
    consumerApp.compose("https://www.egraphs.com" , "login")  should be("https://www.egraphs.com/login")
  }
  
  "getIOSClient" should "correctly forward the boolean value for redirectToItmsLink" in {
    val (app, _) = newConsumerApp()
    
    app.getIOSClient(redirectToItmsLink=true).url should include ("redirectToItmsLink=true")
    app.getIOSClient(redirectToItmsLink=false).url should not include ("redirectToItmsLink")
  }
  
  private def newConsumerApp(baseUrl: String = "https://www.egraphs.com"): (ConsumerApplication, ConfigFileProxy) = {
    val config = mock[ConfigFileProxy]
    
    config.applicationBaseUrl returns baseUrl
    (new ConsumerApplication(config), config)
  }
}