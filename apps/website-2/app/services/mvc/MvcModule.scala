package services.mvc

import celebrity.{UpdateCatalogStarsActor, CatalogStarsActor, CelebrityViewConverting, CelebrityViewConversions}
import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule

/**
 * Guice bindings and bootstrapping for functionality associated with converting domain model
 * objects into corresponding front-end ViewModels.
 **/
object MvcModule extends AbstractModule with ScalaModule {
  /** Initializes some relevant actors */
  def init() {
    CatalogStarsActor.singleton.start()
    UpdateCatalogStarsActor.init()
  }

  //
  // AbstractModule members
  //
  override def configure() {
    bind[CelebrityViewConverting].toInstance(CelebrityViewConversions)
  }

}