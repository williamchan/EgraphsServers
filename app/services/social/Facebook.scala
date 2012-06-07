package services.social

import services.http.PlayConfig
import java.util.Properties
import com.google.inject.{Provider, Inject}

/**
 * Provides our Facebook App ID to Guice as an injectable string
 *
 * Usage:
 * {{{
 *   class MyClassThatUsesFacebook @Inject() (@FacebookAppId fbAppId: String) {
 *     // Do something with the string in here
 *   }
 * }}}
 */
private[social] class FacebookAppIdProvider @Inject()(@PlayConfig playConfig: Properties) extends Provider[String] {
  def get(): String = {
    playConfig.getProperty("fb.appid")
  }
}