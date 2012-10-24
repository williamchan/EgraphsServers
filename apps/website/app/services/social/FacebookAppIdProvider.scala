package services.social

import com.google.inject.Inject
import services.inject.InjectionProvider
import services.config.ConfigFileProxy

class FacebookAppIdProvider @Inject()(config: ConfigFileProxy) extends InjectionProvider[String] {
  override def get() = {
    config.fbAppid
  }
} 
