package services.social

import egraphs.playutils.Encodings.URL

object Pinterest {

  /**
   * See "Pin It Button for Web Sites" section of http://pinterest.com/about/goodies/
   * @param url url to pin
   * @param media media associated with the pin
   * @param description description of the pin
   * @return a url that contains everything Pinterest needs to know to create a pin
   */
  def getPinterestShareLink(url: String, media: String, description: String): String = {
    "http://pinterest.com/pin/create/button/?url=" + URL.encode(url) +
      "&media=" + URL.encode(media) +
      "&description=" + URL.encode(description)
  }

}
