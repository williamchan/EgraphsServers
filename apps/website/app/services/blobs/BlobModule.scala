package services.blobs

import uk.me.lings.scalaguice.ScalaModule
import com.google.inject.{Provider, Singleton, Guice, AbstractModule}

object  BlobModule extends AbstractModule with ScalaModule {
  override def configure() {
    bind[BlobVendor].toProvider[BlobVendorProvider]
    bind[Blobs].in[Singleton]
  }
}