package services.social

import utils.{DBTransactionPerTest, ClearsDatabaseAndValidationBefore, TestData, EgraphsUnitTest}
import models.FulfilledOrder
import models.enums.EgraphState

class SocialTests extends EgraphsUnitTest
  with ClearsDatabaseAndValidationBefore
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
    var celebrity = TestData.newSavedCelebrity()
    var link = Twitter.getEgraphShareLink(celebrity = celebrity, viewEgraphUrl = "myegraph")
    link should include("url=myegraph")
    link should include("text=Check out this choice egraph from " + celebrity.publicName)

    celebrity = celebrity.copy(twitterUsername = Some("egraphceleb")).save()
    link = Twitter.getEgraphShareLink(celebrity = celebrity, viewEgraphUrl = "myegraph")
    link should include("url=myegraph")
    link should include("text=Hey @egraphceleb this is one choice egraph you made.")
  }

}
