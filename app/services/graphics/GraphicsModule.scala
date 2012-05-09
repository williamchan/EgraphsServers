package services.graphics

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.AbstractModule

/**
 * Dependency Injection bindings for graphics services.
 */

object GraphicsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[() => GraphicsSource].toInstance(() => SVGZGraphicsSource(0, 0))
  }
}
