package services.social

import utils.{DBTransactionPerTest, ClearsCacheBefore, TestData, EgraphsUnitTest}
import models.FulfilledOrder
import models.enums.EgraphState
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class SocialTests extends EgraphsUnitTest
  with ClearsCacheBefore
  with DBTransactionPerTest
{

  "Facebook" should "return getEgraphShareLink" in {
    val egraph = TestData.newSavedEgraph().withEgraphState(EgraphState.Published).save()
    val order = egraph.order.copy(recipientName = "recipientName").save()

    val link = Facebook.getEgraphShareLink(fbAppId = "fbAppId", fulfilledOrder = FulfilledOrder(order, egraph), thumbnailUrl = "mythumb", viewEgraphUrl = "myegraph")
    link should include("redirect_uri=myegraph")
    link should include("link=myegraph")
    link should include("picture=mythumb")
    link should include("name=" + order.product.celebrity.publicName + " egraph for recipientName")
    link should include("link=myegraph")
  }

  "Twitter" should "return getEgraphShareLink" in {
    val celebrity = TestData.newSavedCelebrity()
    val customer = TestData.newSavedCustomer()
    val (order, egraph) = TestData.newFulfilledOrder(customer)
    val link = Twitter.getEgraphShareLink(celebrity = celebrity, order = order, viewEgraphUrl = "myegraph")
    link should include("url=myegraph")
    link should include("text=An egraph for " + customer.name + " from " + celebrity.publicName)

    val twitterCeleb = celebrity.copy(twitterUsername = Some("egraphceleb")).save()
    val (order1, egraph1) = TestData.newFulfilledOrder(customer)
    val twitterLink = Twitter.getEgraphShareLink(celebrity = twitterCeleb, order = order1, viewEgraphUrl = "myegraph")
    twitterLink should include("url=myegraph")
    twitterLink should include("text=An egraph for " + customer.name + " from @" + twitterCeleb.twitterUsername.getOrElse(""))
  }

  "Pinterest" should "return url for Pin It button" in {
    Pinterest.getPinterestShareLink(
      url = "https://www.egraphs.com/66",
      media = "https://d3kp0rxeqzwisk.cloudfront.net/assets/images/egraph_plaque-fp-2697217610.png",
      description = "Pedro Martinez egraph for Tommy Shannon Jr"
    ) should be("http://pinterest.com/pin/create/button/?url=https%3A%2F%2Fwww.egraphs.com%2F66&media=https%3A%2F%2Fd3kp0rxeqzwisk.cloudfront.net%2Fassets%2Fimages%2Fegraph_plaque-fp-2697217610.png&description=Pedro+Martinez+egraph+for+Tommy+Shannon+Jr")
  }
}
