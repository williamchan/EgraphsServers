package services.mvc.egraphs

import models._
import frontend.footer.FooterData
import frontend.header.HeaderData
import services.graphics.Handwriting
import services.ConsumerApplication
import play.api.templates.Html
import models.frontend.egraph.{LandscapeEgraphFrameViewModel, PortraitEgraphFrameViewModel}
import services.blobs.AccessPolicy
import models.Egraph
import models.Order
import _root_.frontend.formatting.DateFormatting.Conversions._
import services.social.{Twitter, Facebook}
import egraphs.authtoken.AuthenticityToken

/**
 * Object for rendering the egraph page.
 */
object EgraphView {
  def renderEgraphPage(
    egraph: Egraph,
    order: Order,
    penWidth: Double=Handwriting.defaultPenWidth,
    shadowX: Double=Handwriting.defaultShadowOffsetX,
    shadowY: Double=Handwriting.defaultShadowOffsetY,
    facebookAppId: String = "",
    galleryLink: Option[String] = None,
    consumerApp: ConsumerApplication
   )(implicit
     headerData: HeaderData,
     footerData: FooterData,
     authToken: AuthenticityToken
    ) : Html = {

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
    .withPenWidth(penWidth)
    .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
    .withPenShadowOffset(shadowX, shadowY)
    .scaledToWidth(frame.imageWidthPixels)

    val svgzImageUrl = frameFittedImage.getSavedUrl(AccessPolicy.Public)
    val rasterImageUrl = frameFittedImage.rasterized.getSavedUrl(AccessPolicy.Public)

    // Prepare the icon
    val icon = product.icon
    val frameFittedIconUrl = icon.resized(Product.minIconWidth, Product.minIconWidth).getSaved(AccessPolicy.Public).url

    // Prepare the story
    val story = egraph.story(celebrity, product, order)

    // Signed at date
    val formattedSigningDate = egraph.getSignedAt.formatDayAsPlainLanguage

    // Social links
    val thisPageLink = consumerApp.absoluteUrl(controllers.routes.WebsiteControllers.getEgraphClassic(order.id).url)

    val facebookShareLink = Facebook.getEgraphShareLink(fbAppId = facebookAppId,
    fulfilledOrder = FulfilledOrder(order = order, egraph = egraph),
    thumbnailUrl = rasterImageUrl,
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
