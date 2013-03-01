package services.social

import models.Celebrity
import models.Order

object Twitter {

  /**
   * @param celebrity Celebrity
   * @param viewEgraphUrl url to egraph page
   * @return a link with everything Twitter needs to make a tweet
   */
  def getEgraphShareLink(celebrity: Celebrity, order: Order, viewEgraphUrl: String): String = {
    views.frontend.Utils.getTwitterShareLink(link = viewEgraphUrl, text = getTweetText(celebrity, order))
  }

  def getTweetText(celebrity: Celebrity, order: Order): String = {
    val celebNameToAppearInTweet = if(celebrity.twitterUsername.isDefined && !celebrity.doesNotHaveTwitter) {
      "@" + celebrity.twitterUsername.get
    } else {
      celebrity.publicName
    }
    "An egraph for " + order.recipientName + " from " + celebNameToAppearInTweet
  }
}
