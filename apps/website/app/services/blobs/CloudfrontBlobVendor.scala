package services.blobs

import com.google.inject.Inject
import services.cache.CacheFactory
import java.net.URL
import services.logging.Logging

/**
 * BlobVendor decorator for providing urls that map to the Amazon Cloudfront Content Distribution Network (CDN)
 * The underlying blobVendor must be hosting its data at an endpoint that has a cloudfront server pointing to it.
 * More info available here: http://aws.amazon.com/cloudfront/faqs/#What_is_Amazon_CloudFront
 * @param domain The domain of the cloudfront endpoint being used to cache files at the edge of the CDN
 * @param blobVendorDelegate The class that this vendor is decorating.
 */
class CloudfrontBlobVendor @Inject()(domain: String, override protected val blobVendorDelegate: BlobVendor)
  extends BlobVendorComposition
{
  override def urlOption(namespace: String, key: String) = {
    val delegateResponse = blobVendorDelegate.urlOption(namespace, key)

    delegateResponse.map( urlString =>
      {
        val url = new URL(urlString)
        "http://" + domain + url.getPath
      }
    )
  }

}

object CloudfrontBlobVendor extends Logging
