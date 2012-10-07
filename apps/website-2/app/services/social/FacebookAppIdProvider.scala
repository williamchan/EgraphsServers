package services.social

import com.google.inject.Inject

import play.api.Configuration
import services.inject.InjectionProvider

class FacebookAppIdProvider @Inject()(config: Configuration) extends InjectionProvider[String] {
  override def get() = {
    config.getString("fb.appid").get
  }
} 
