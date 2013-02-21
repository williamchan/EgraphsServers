package services.graphics

import net.codingwell.scalaguice.ScalaModule
import com.google.inject.AbstractModule

/**
 * Dependency Injection bindings for graphics services.
 */

object GraphicsModule extends AbstractModule with ScalaModule {
  def configure() {
    bind[() => GraphicsSource].toInstance(() => SVGZGraphicsSource(0, 0))
    bind[() => RasterGraphicsSource].toInstance(() => RasterGraphicsSource(0,0))
  }
}

