package services.blobs

import com.google.inject.Inject
import java.net.URL
import services.Time.IntsToSeconds._

/**
 * BlobVendor decorator for providing urls that map to the Amazon Cloudfront Content Distribution Network (CDN)
 * The underlying blobVendor must be hosting its data at an endpoint that has a cloudfront server pointing to it.
 * More info available on the [[http://aws.amazon.com/cloudfront/faqs/#What_is_Amazon_CloudFront cloudfront docs]]
 *
 * @param domain The domain of the cloudfront endpoint being used to cache files at the edge of the CDN
 * @param blobVendorDelegate The class that this vendor is decorating.
 */
class CloudfrontBlobVendor @Inject()(domain: String, override protected val blobVendorDelegate: BlobVendor)
  extends BlobVendorComposition
{
  override def urlOption(namespace: String, key: String) = {
    val delegateResponse = blobVendorDelegate.urlOption(namespace, key)

    delegateResponse.map { urlString =>
      val url = new URL(urlString)
      "https://" + domain + url.getPath
    }
  }
  
  override def secureUrlOption(namespace: String, key: String, expirationSeconds: Int = 5 minutes): Option[String] = {
    blobVendorDelegate.secureUrlOption(namespace, key, expirationSeconds)
  }
}
