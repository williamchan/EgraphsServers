package services.mvc

import com.google.inject.AbstractModule

import celebrity.CelebrityViewConversions
import celebrity.CelebrityViewConverting
import celebrity.UpdateCatalogStarsActor
import net.codingwell.scalaguice.ScalaModule

/**
 * Guice bindings and bootstrapping for functionality associated with converting domain model
 * objects into corresponding front-end ViewModels.
 **/
object MvcModule extends AbstractModule with ScalaModule {
  //
  // AbstractModule members
  //
  override def configure() {
    bind[CelebrityViewConverting].toInstance(CelebrityViewConversions)
  }

}