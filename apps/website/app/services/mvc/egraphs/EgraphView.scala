package services.mvc.egraphs

import models._
import frontend.footer.FooterData
import frontend.header.HeaderData
import services.ConsumerApplication
import play.api.templates.Html
import models.frontend.egraph.{LandscapeEgraphFrameViewModel, PortraitEgraphFrameViewModel}
import services.blobs.AccessPolicy
import models.Egraph
import models.Order
import _root_.frontend.formatting.DateFormatting.Conversions._
import services.social.{Pinterest, Twitter, Facebook}
import egraphs.authtoken.AuthenticityToken
import services.graphics.Handwriting
import services.video.EgraphVideoEncoder

/**
 * Object for rendering the egraph page.
 */
object EgraphView {

  def renderEgraphPage(
    egraph: Egraph,
    order: Order,
    consumerApp: ConsumerApplication
  )(implicit
    headerData: HeaderData,
    footerData: FooterData,
    authToken: AuthenticityToken
  ): Html = {
    val product = order.product
    val celebrity = product.celebrity
    val mp4Url = egraph.getVideoAsset.getSavedUrl(AccessPolicy.Public)
    val egraphStillUrl = egraph.getEgraphImage(EgraphVideoEncoder.canvasWidth).asJpg.getSavedUrl(AccessPolicy.Public)
    val thisPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)
    val iframeUrl = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphPlayerEmbed(order.id).url)
    val classicPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphClassic(order.id).url)
    val tweetText = Twitter.getTweetText(celebrity)
    val shareOnPinterestLink = Pinterest.getPinterestShareLink(
      url = thisPageLink,
      media = egraphStillUrl,
      description = celebrity.publicName + " egraph for " + order.recipientName)
    views.html.frontend.egraph(
      mp4Url = mp4Url,
      videoPosterUrl = egraphStillUrl,
      celebrityName = celebrity.publicName,
      celebrityTagline = celebrity.roleDescription,
      recipientName = order.recipientName,
      privacySetting = order.privacyStatus.name,
      messageToCelebrity = order.messageToCelebrity,
      productIconUrl = product.iconUrl,
      signedOnDate = egraph.getSignedAt.formatDayAsPlainLanguage,
      thisPageLink = thisPageLink,
      classicPageLink = classicPageLink,
      shareOnPinterestLink = shareOnPinterestLink,
      tweetText = tweetText,
      isPromotional = order.isPromotional,
      iframeUrl = iframeUrl
    )
  }

  def renderEgraphClassicPage(
    egraph: Egraph,
    order: Order,
    facebookAppId: String = "",
    galleryLink: Option[String] = None,
    consumerApp: ConsumerApplication
  )(implicit
    headerData: HeaderData,
    footerData: FooterData,
    authToken: AuthenticityToken
  ): Html = {

    // Get related data model objects
    val product = order.product
    val celebrity = product.celebrity

    // Prepare the framed image
    val frame = product.frame match {
      case PortraitEgraphFrame => PortraitEgraphFrameViewModel
      case LandscapeEgraphFrame => LandscapeEgraphFrameViewModel
    }

    val rawSignedImage = egraph.image(product.photoImage)
    // TODO SER-170 this code is quite similar to that in GalleryOrderFactory.
    // Refactor together and put withSigningOriginOffset inside EgraphImage.
    val frameFittedImage = rawSignedImage
      .withPenWidth(Handwriting.defaultPenWidth)
      .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
      .withPenShadowOffset(Handwriting.defaultShadowOffsetX, Handwriting.defaultShadowOffsetY)
      .scaledToWidth(frame.imageWidthPixels)
    val svgzImageUrl = frameFittedImage.getSavedUrl(AccessPolicy.Public)
    val pngImageUrl = frameFittedImage.asPng.getSavedUrl(AccessPolicy.Public)

    // Prepare the icon
    val frameFittedIconUrl = product.icon.resized(Product.minIconWidth, Product.minIconWidth).getSaved(AccessPolicy.Public).url

    // Prepare the story
    val story = egraph.story(celebrity, product, order)

    // Signed at date
    val formattedSigningDate = egraph.getSignedAt.formatDayAsPlainLanguage

    // Social links
    val thisPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraph(order.id).url)

    val facebookShareLink = Facebook.getEgraphShareLink(fbAppId = facebookAppId,
      fulfilledOrder = FulfilledOrder(order = order, egraph = egraph),
      thumbnailUrl = pngImageUrl,
      viewEgraphUrl = thisPageLink)

    val twitterShareLink = Twitter.getEgraphShareLink(celebrity = celebrity, viewEgraphUrl = thisPageLink)

    views.html.frontend.egraph_classic(
      signerName = celebrity.publicName,
      recipientName = order.recipientName,
      frameCssClass = frame.cssClass,
      frameLayoutColumns = frame.cssFrameColumnClasses,
      productIcon = frameFittedIconUrl,
      storyLayoutColumns = frame.cssStoryColumnClasses,
      storyTitle = product.storyTitle,
      storyBody = story.body,
      audioUrl = egraph.assets.audioMp3Url,
      signedImage = svgzImageUrl,
      signedOnDate = formattedSigningDate,
      shareOnFacebookLink = facebookShareLink,
      shareOnTwitterLink = twitterShareLink,
      galleryLink = galleryLink,
      isPromotional = order.isPromotional
    )
  }
}
