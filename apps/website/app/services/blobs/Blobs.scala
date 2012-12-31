package services.blobs

import org.jclouds.blobstore.{BlobStoreContext, BlobStore}
import org.jclouds.io.Payload
import java.io._
import com.google.inject.Inject
import services.logging.Logging
import org.jclouds.blobstore.domain.Blob
import services.Time
import Time.IntsToSeconds._
import services.AppConfig
import services.config.ConfigFileProxy

/**
 * Convenience methods for storing and loading large binary data: images,
 * audio, signatures, video, etc.
 *
 * Provides Amazon-based or filesystem-based implementation based on the "blobstore" value
 * in application.conf
 *
 * Also pimps out the jclouds Blob library to include some useful data transformations to/from
 * Blobs, Payloads, Array[Byte]s, Strings, Files, and Streams.
 *
 * To use, import Blobs.Conversions._ into whatever scope.
 */
class Blobs @Inject() (
  blobVendor: BlobVendor,
  blobVendorProvider: BlobVendorProvider,
  config: ConfigFileProxy
) extends Logging 
{
  import Blobs._

  /**
   * Tests whether a blob with the provided key exists
   *
   * @param key the key to test
   * @return true that a blob with the given key exists
   */
  def exists(key: String): Boolean = {
    blobVendor.exists(blobstoreNamespace, key)
  }

  /**
   * Retrieve the Blob at a given key.
   */
  def get(key: String): Option[Blob] = {
    blobVendor.get(blobstoreNamespace, key)
  }

  /**
   * Retrieve the Blob at a given key from the static resources blobstore. Always attempts to access Amazon S3.
   * Only use this method if you intend on using the bytes here on the server. Otherwise, for example if you intend 
   * a browser to be downloading the file, use getStaticResourceUrl.
   */
  def getStaticResource(key: String): Option[Blob] = {
    val store = s3.context.getBlobStore
    Option(store.getBlob(staticResourceBlobstoreNamespace, key))
  }

  /**
   * Returns a short-lived signed URL to the blob location denoted by the key parameter in the static resource S3 bucket.
   * This URL follows the query string authentication specification as described in this documentation:
   * http://s3.amazonaws.com/doc/s3-developer-guide/RESTAuthentication.html. Note that a URL will be returned even if
   * the key does not point to any resource, ie passing in "IDoNotExist" as the key will still return a signed URL.
   */
  def getStaticResourceUrl(key: String, expirationSeconds: Int = 5 minutes): String = {
    s3.secureUrlOption(staticResourceBlobstoreNamespace, key, expirationSeconds).get
  }

  /**
   * UNSAFE. Retrieves the URL for the provided key. Favor getUrlOption
   */
  def getUrl(key: String): String = {
    getUrlOption(key).get
  }

  /**
   * Returns the public URL for the Blob. The URL will only work if
   * the blob stored is publicly available.
   */
  def getUrlOption(key: String): Option[String] = {
    blobVendor.urlOption(blobstoreNamespace, key)
  }

  /**
   * Returns the secure URL for the Blob.
   */
  def getSecureUrlOption(key: String, expirationSeconds: Int = 5 minutes): Option[String] = {
    blobVendor.secureUrlOption(blobstoreNamespace, key, expirationSeconds)
  }

  /**
   * Puts data into a Blob object.
   *
   * @param key the key by which to store the data
   * @param bytes the data to store
   * @param access the Blobstore access policy for this object. This will only be enforced
   *   against Amazon S3.
   */
  def put(key: String, bytes: Array[Byte], access: AccessPolicy=AccessPolicy.Private) {
    if(bytes == null || bytes.isEmpty) {
      delete(key)
    }

    blobVendor.put(blobstoreNamespace, key, bytes, access)
  }

  /** Delete the data at a certain key. */
  def delete(key: String) {
    blobVendor.delete(blobstoreNamespace, key)
  }

  /** Clears the entire Blobstore. For God's sake don't do this in production. */
  def scrub() {
    val applicationMode = config.applicationMode
    log("Checking application.mode before scrubbing blobstore. Must be in dev mode. Mode is: " + applicationMode)
    if (applicationMode != "dev" ||
        config.applicationId != "test" ||
        config.blobstoreVendor != "filesystem") {
      throw new IllegalStateException("Cannot scrub blobstore unless in dev mode, application is test, and blobstore is filesystem")
    }

    if (config.blobstoreAllowScrub) {
      blobStore.clearContainer(blobstoreNamespace)
    } else {
      throw new IllegalStateException(
        """I'm not going to scrub the blobstore unless "blobstore.allowscrub"
        is set to "yes" in application.conf"""
      )
    }
  }

  /**
   * Retrieve the actual Blobstore when more intricate use of the
   * jclouds api is needed.
   */
  def blobStore: BlobStore = {
    blobVendor.context.getBlobStore
  }

  /**
   * Verifies configuration information and bootstraps the egraphs container
   * in the blobstore.
   */
  def init() = {
    checkAvailable()

    if (!blobStore.containerExists(blobstoreNamespace)) {
      blobStore.createContainerInLocation(null, blobstoreNamespace)
    }
  }

  /**
   * Retrieve the jclouds BlobStore context when more intricate use of the
   * jclouds api is needed.
   */
  def context: BlobStoreContext = {
    blobVendor.context
  }

  /**
   * Verifies that the blobstore is correctly configured, as far as it can
   * without actually trying to open the store.
   */
  def checkAvailable() {
    require(
      blobstoreType != null,
      """
      application.conf: A "blobstore" configuration value of either "s3" or
      "filesystem" is required
      """
    )

    require(
      blobstoreNamespace != null,
      """
      application.conf: A "blobstore.namespace" value is required. On s3 this
      is the bucket name.
      """
    )

    blobVendor.checkConfiguration()
  }

  //
  // Private members
  //
  private def s3: S3BlobVendor = {
    blobVendorProvider.s3
  }
}

/**
 * Provides the active BlobVendor to Guice, which is dictated by the "blobstore" value in
 * application.conf
 */

object Blobs {
  /** Application configuration */
  private[blobs] val configuration = AppConfig.instance[ConfigFileProxy]

  /** Type of blobstore. See "blobstore" in application.conf */
  private[blobs] val blobstoreType = configuration.blobstoreVendor

  /** Namespace of blobstore; equivalent to S3's bucket */
  private[blobs] val blobstoreNamespace = configuration.blobstoreNamespace

  /** Namespace of blobstore that holds static resources; equivalent to S3's bucket */
  private[blobs] val staticResourceBlobstoreNamespace = configuration.staticResourcesBlobstoreNamespace

  /**
   * Gives Blobs access to RichPayload. Also allows more variance in inputs to put()
   * (e.g. Strings and Files)
   */
  object Conversions {
    implicit def blobToRichPayload(blob: Blob): RichPayload = {
      payloadToRichPayload(blob.getPayload)
    }

    implicit def payloadToRichPayload(payload: Payload): RichPayload = {
      new RichPayload(payload)
    }

    implicit def stringToByteArray(string: String): Array[Byte] = {
      string.getBytes
    }

    implicit def fileToByteArray(file: File): Array[Byte] = {
      val bytes = new Array[Byte](file.length.toInt)
      new BufferedInputStream(new FileInputStream(file)).read(bytes)

      bytes
    }
  }

  /**
   * Pimping of the jclouds Blob library to make content access easier.
   */
  class RichPayload(payload: Payload) {

    def asInputStream: BufferedInputStream = {
      new BufferedInputStream(payload.getInput)
    }

    def asIntStream: Stream[Int] = {
      val stream = asInputStream

      Stream
        .continually(stream.read)
        .takeWhile(theInt => theInt != -1)
    }

    def asByteStream: Stream[Byte] = {
      asIntStream.map(theInt => theInt.toByte)
    }

    def asByteArray: Array[Byte] = {
      val bytesOut = new ByteArrayOutputStream()

      val is = asInputStream
      var nextByte = is.read()

      while (nextByte != -1) {
        bytesOut.write(nextByte)
        nextByte = is.read()
      }

      is.close()
      bytesOut.toByteArray
    }

    def asString: String = {
      new String(asByteArray, "UTF-8")
    }
  }
}

