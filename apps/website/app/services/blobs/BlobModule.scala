package services.blobs

import net.codingwell.scalaguice.ScalaModule
import com.google.inject.{Provider, Singleton, Guice, AbstractModule}

object BlobModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[BlobVendor].toProvider[BlobVendorProvider].in[Singleton]
    bind[Blobs].in[Singleton]
  }
}