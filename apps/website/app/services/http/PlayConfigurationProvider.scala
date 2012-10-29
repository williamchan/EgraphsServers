package services.http

import com.google.inject.Inject

import play.api.Configuration
import services.inject.InjectionProvider

/**
 * Provides access to the Play configuration object for the currently running
 * application stage (e.g. "live", "demo", etc)
 * 
 * Behavior is that, for any given configuration key, the application stage's
 * version will be returned first, followed by any default un-namespaced
 * version. For example, given the following config file:
 * 
 *   mailProvider = mock
 *   %live.mailProvider = gmail
 * 
 * the live running application's mail Provider would first resolve to 
 * "gmail". And if that key had not been there, it would have returned mock.
 */
private[http] class PlayConfigurationProvider @Inject()(@PlayId playId: String) 
  extends InjectionProvider[Configuration] 
{
  override def get(): Configuration = {
    val baseConfig = play.api.Play.current.configuration    
    
    if (playId == "test") {
      baseConfig
    } else {
       val stageConfig = baseConfig.getConfig("%" + playId).getOrElse {
         throw new IllegalArgumentException("No configurations found for play ID " + playId)
       }

       baseConfig ++ stageConfig
    }
  }
}