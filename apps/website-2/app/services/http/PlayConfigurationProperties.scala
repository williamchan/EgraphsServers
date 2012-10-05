package services.http

import play.api.Play
import java.util.Properties

/**
 * Creates a Properties that maps to Play 2.0's configuration file.
 * 
 * This should eventually just access the configuration object directly,
 * but is relic for back when Play 1.0 provided configurations as a Properties
 * object.
 * 
 **/
object PlayConfigurationProperties {  

  lazy val properties: Properties = {
    val props = new Properties()

    val config = Play.current.configuration
    
    for (configKey <- config.keys) {
      try {
        config.getString(configKey).map(value => props.put(configKey, value.toString))
      } catch {
        case error =>
      }    
    }

    props
  }
}