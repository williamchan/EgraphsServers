package services.http.twitter

import org.specs2.mock.Mockito

import com.google.inject.Inject

import services.config.ConfigFileProxy
import services.inject.InjectionProvider
import twitter4j.api.UsersResources
import twitter4j.conf.ConfigurationBuilder
import twitter4j.TwitterFactory

/**
 * Provides the active Twitter to Guice, which is dictated by the "twitter" value in
 * application.conf
 */
class TwitterProvider @Inject() (
  config: ConfigFileProxy) extends Mockito with InjectionProvider[UsersResources] {
  private val twitterType = config.twitter

  def get() = {
    twitterType match {
      case "fake" =>
        // benign null pointers are expected in test from this, there isn't an easy way around this, I tried. - Myyk
        mock[UsersResources]

      case "twitter4j" =>
        twitterFactory.getInstance

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"twitter\" value \"" + unknownType + "\" not supported.")
    }
  }

  private val twitterFactory = {
    val cb = new ConfigurationBuilder()
    cb.setDebugEnabled(true)
      .setOAuthConsumerKey("C8VdC6rJZSply1UwSoawQ")
      .setOAuthConsumerSecret("kQGkRU3FODpJ3jiSkXmXIYJa5mr5GpGEgdxcWohSM")
      .setOAuthAccessToken("349117245-QbpDqvuDhUA3sd7Kjjk9ieyWvoDNP6hUFxpEHl2v")
      .setOAuthAccessTokenSecret("mDMeBx0qhBbqg7x3rQvdXMvUfLx5nkq8bfGWd4y9xDY")
    new TwitterFactory(cb.build())
  }
}
