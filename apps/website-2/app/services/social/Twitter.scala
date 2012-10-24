package services.social

import models.Celebrity

object Twitter {

  /**
   * @param celebrity Celebrity
   * @param viewEgraphUrl url to egraph page
   * @return a link with everything Twitter needs to make a tweet
   */
  def getEgraphShareLink(celebrity: Celebrity, viewEgraphUrl: String): String = {
    val tweetTextIfCelebHasTwitterName = celebrity.twitterUsername.map { celebTwitterName =>
      "Hey @" + celebTwitterName + " this is one choice egraph you made."
    }
    val tweetText = tweetTextIfCelebHasTwitterName.getOrElse {
      "Check out this choice egraph from " + celebrity.publicName + "."
    }
    views.frontend.Utils.getTwitterShareLink(link = viewEgraphUrl, text = tweetText)

  }

}
