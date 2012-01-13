package libs

import java.util.Properties
import org.jclouds.filesystem.reference.FilesystemConstants
import play.Play.configuration
import org.jclouds.blobstore.{BlobStoreContextFactory, BlobStoreContext, BlobStore}
import org.jclouds.blobstore.domain.Blob
import org.jclouds.io.Payload
import org.jclouds.aws.s3.AWSS3Client
import org.jclouds.s3.domain.CannedAccessPolicy
import java.io._
import play.mvc.Http.Request

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
object Blobs {
  /**
   * Tests whether a blob with the provided key exists
   *
   * @param key the key to test
   * @return true that a blob with the given key exists
   */
  def exists(key: String): Boolean = {
    blobStore.blobExists(blobstoreNamespace, key)
  }

  /**
   * Retrieve the Blob at a given key.
   */
  def get(key: String): Option[Blob] = {
    blobStore.getBlob(blobstoreNamespace, key) match {
      case null =>
        None

      case blob =>
        Some(blob)
    }
  }

  // TODO(wchan): Where to put this?
  def getStaticResource(key: String) : Option[Blob] = {
    blobStore.getBlob(staticResourceBlobstoreNamespace, key) match {
      case null =>
        None

      case blob =>
        Some(blob)
    }
  }

  /**
   * Returns the public URL for the Blob. The URL will only work if
   * the blob was stored with Public.
   */
  def getUrl(key: String): String = {
    blobProvider.urlBase + "/" + key
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
    blobProvider.put(blobstoreNamespace, key, bytes, access)
  }

  /** Delete the data at a certain key. */
  def delete(key: String) {
    blobStore.removeBlob(blobstoreNamespace, key)
  }

  /**
   * Clears the entire Blobstore. Only runs if "blobstore.allowscrub" is
   * set to "yes" in application.conf.
   */
  def scrub() {
    configuration.get("blobstore.allowscrub") match {
      case "yes" =>
        blobStore.clearContainer(blobstoreNamespace)

      case _ =>
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
    context.getBlobStore
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
    blobProvider.context
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

    blobProvider.checkConfiguration()
  }

  /**
   * Provides the active BlobProvider, which is dictated by the "blobstore" value in
   * application.conf
   */
  private def blobProvider: BlobProvider = {
    blobstoreType match {
      case "s3" =>
        S3BlobProvider

      case "filesystem" =>
        FileSystemBlobProvider

      case unknownType =>
        throw new IllegalStateException(
          "application.conf: \"blobstore\" value \"" + unknownType + "\" not supported."
        )
    }
  }

  /** Type of blobstore. See "blobstore" in application.conf */
  private val blobstoreType = configuration.getProperty("blobstore")

  /** Namespace of blobstore; equivalent to S3's bucket */
  private val blobstoreNamespace = configuration.getProperty("blobstore.namespace")

  /** Namespace of blobstore that holds static resources; equivalent to S3's bucket */
  private val staticResourceBlobstoreNamespace = configuration.getProperty("staticresources.blobstore.namespace")

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
   * Constants that determine how Blobs can be accessed.
   */
  sealed trait AccessPolicy

  object AccessPolicy {
    /** Accessible via a public URL */
    case object Public extends AccessPolicy

    /** Inaccessible via public URL */
    case object Private extends AccessPolicy
  }
  
  /**
   * Interface for different BlobStore implementations.
   */
  private trait BlobProvider {
    /** Instantiates a new configured BlobStoreContext */
    def context: BlobStoreContext

    /** Returns the base URL for accessing [[libs.Blobs.AccessPolicy.Public]] resources */
    def urlBase: String

    /**
     * Checks that the blobstore implementation is correctly configured, and throws runtime
     * exceptions if not.
     */
    def checkConfiguration()

    /**
     * Puts an object into a key with a particular namespace.
     *
     * @param namespace the key namespace -- equivalent to an Amazon S3 bucket or a file-system folder.
     *
     * @param key the unique key name against which to store the object.
     *
     * @param data the object data to store.
     */
    def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy)
  }

  /** [[libs.Blobs.BlobProvider]] implementation backed by Amazon S3 */
  private object S3BlobProvider extends BlobProvider {
    //
    // BlobProvider members
    //
    override val urlBase = "http://s3.amazonaws.com" + "/" + blobstoreNamespace
    override def context = {
      new BlobStoreContextFactory().createContext("aws-s3", s3id, s3secret)
    }

    override def put(namespace: String, key: String, data: Array[Byte], access: AccessPolicy) {
      import org.jclouds.s3.options.PutObjectOptions.Builder.withAcl

      val s3:AWSS3Client = context.getProviderSpecificContext.getApi

      val s3Object = s3.newS3Object()

      s3Object.getMetadata.setKey(key)
      s3Object.setPayload(data)

      s3.putObject(namespace, s3Object, withAcl(s3ConstantForAccessPolicy(access)))
    }

    override def checkConfiguration() {
      require(
        s3id != null,
        """
        application.conf: An "s3.id" configuration must be provided.
        """
      )
      require(
        s3secret != null,
        """
        application.conf: An "s3.secret" configuration must be provided.
        """
      )
    }

    //
    // Private members
    //
    /** Amazon S3 public id */
    private val s3id = configuration.getProperty("s3.id")

    /** Amazon S3 secret key */
    private val s3secret = configuration.getProperty("s3.secret")

    private def s3ConstantForAccessPolicy(access: AccessPolicy): CannedAccessPolicy = {
      access match {
        case AccessPolicy.Public => CannedAccessPolicy.PUBLIC_READ
        case AccessPolicy.Private => CannedAccessPolicy.PRIVATE
      }
    }

  }

  /**
   * [[libs.Blobs.BlobProvider]] implementation for the file system.
   */
  private object FileSystemBlobProvider extends BlobProvider {
    //
    // BlobProvider members
    //
    override val urlBase = Request.current().getBase + "/test/files"
    override def context = {
      val properties = new Properties

      properties.setProperty(FilesystemConstants.PROPERTY_BASEDIR, "./tmp/blobstore")

      // jclouds borks if we don't provde any "jclouds.credential" property.
      properties.setProperty("jclouds.credential", "It doesn't matter what this value is.")

      new BlobStoreContextFactory().createContext("filesystem", properties)
    }

    override def put(namespace: String, key: String, bytes: Array[Byte], access: AccessPolicy) {
      val blobStore = context.getBlobStore

      blobStore.putBlob(
        namespace,
        blobStore.blobBuilder(key).payload(bytes).build()
      )
    }
    override def checkConfiguration() { }
  }
}
