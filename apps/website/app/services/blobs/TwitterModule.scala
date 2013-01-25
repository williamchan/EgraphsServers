package services.blobs

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{ Singleton, AbstractModule }
import services.http.twitter.TwitterProvider
import twitter4j.api.UsersResources

object TwitterModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[UsersResources].toProvider[TwitterProvider].in[Singleton]
  }
}