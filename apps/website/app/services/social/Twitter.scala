package services.social

import models.Celebrity

object Twitter {

  /**
   * @param celebrity Celebrity
   * @param viewEgraphUrl url to egraph page
   * @return a link with everything Twitter needs to make a tweet
   */
  def getEgraphShareLink(celebrity: Celebrity, viewEgraphUrl: String): String = {
    views.frontend.Utils.getTwitterShareLink(link = viewEgraphUrl, text = getTweetText(celebrity))
  }

  def getTweetText(celebrity: Celebrity): String = {
    val tweetTextIfCelebHasTwitterName = for {
      celebTwitterName <- celebrity.twitterUsername if (!celebrity.doesNotHaveTwitter)
    } yield {
      "Hey @" + celebTwitterName + " this is one choice egraph you made."
    }
    tweetTextIfCelebHasTwitterName.getOrElse {
      "Check out this choice egraph from " + celebrity.publicName + "."
    }
  }
}
