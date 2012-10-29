package services

import com.google.inject.Inject
import services.config.ConfigFileProxy

class ConsumerApplication @Inject()(config: ConfigFileProxy) {

  /**
   * DO NOT USE play.api.mvc.Http.absoluteURL because it ignores application.baseUrl. We do not want emails directing
   * consumers to admin.egraphs.com.
   *
   * @param urlPath the route of interest
   * @return the absolute URL associated with the action starting with the application's baseUrl, eg if the base url was
   *         "https://www.egraphs.com/" and the action maps to "/login", then "https://www.egraphs.com/login" is returned
   */
  def absoluteUrl(urlPath: String): String = {
    val baseUrl = config.applicationBaseUrl
    compose(baseUrl, urlPath)
  }
  
  /**
   * Why am I writing this? Because the application.baseUrl config parameter seems to have a trailing "/" in all of the
   * Play examples while a url from ActionDefinitions has a leading "/". So, I wanted to make sure that we are using
   * application.baseUrl correctly and also correctly composing absolute Urls.
   *
   * @param baseUrl eg "https://www.egraphs.com/"
   * @param relativeUrl eg "/login"
   * @return the concatenated full Url
   */
  private[services] def compose(baseUrl: String, relativeUrl: String): String = {
    (baseUrl.endsWith("/"), relativeUrl.startsWith("/")) match {
      case (true, true) => baseUrl + relativeUrl.substring(1)
      case (true, false) | (false, true) => baseUrl + relativeUrl
      case (false, false) => baseUrl + "/" + relativeUrl
    }
  }
}
