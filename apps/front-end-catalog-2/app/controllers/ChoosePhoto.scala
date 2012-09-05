package controllers

import play.mvc.Controller

import models.frontend.storefront._
import java.text.SimpleDateFormat
import models.frontend.storefront.ChoosePhotoTileProduct
import models.frontend.storefront.ChoosePhotoRecentEgraph
import models.frontend.storefront.ChoosePhotoCelebrity

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller
  with DefaultHeaderAndFooterData
  with DefaultStorefrontBreadcrumbs
{
  import frontend.formatting.MoneyFormatting.Conversions._

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  /** Renders tiled-view with n products */
  def withProducts(num: Int = 1) = {
    val products = for(i <- 1 to num) yield sampleTile
    ChoosePhotoDefaults().copy(products=products).renderTiles
  }

  /** Renders tiled view with n recent egraphs */
  def withRecentEgraphs(num: Int = 1) = {
    val recentEgraphs = for (i <- 1 to num) yield sampleEgraph
    ChoosePhotoDefaults().copy(recentEgraphs=recentEgraphs).renderTiles
  }

  /** Renders tiled view with n partner icons (e.g. MLB, Tampa Bay Rays, etc) */
  def withPartnerIcons(num: Int=1) = {
    val icons = for (i <- 1 to num) yield samplePartnerIcon
    ChoosePhotoDefaults().copy(partnerIcons=icons).renderTiles
  }

  def tilesWithSoldOut = {
    val products = Seq(
      sampleTile.copy(quantityRemaining = 0 )
    )

    ChoosePhotoDefaults().copy(products=products).renderTiles
  }

  def carouselWithSoldOut = {
    val products = Seq(
      sampleCarouselProduct.copy(quantityRemaining = 0)
    )

    ChoosePhotoDefaults().copy(carouselProducts=products).renderCarousel
  }


  /** Renders tiled view of a celebrity that lacks a twitter handle */
  def withoutTwitterHandle = {
    val defaults = ChoosePhotoDefaults()
    ChoosePhotoDefaults().copy(
      celeb=defaults.celeb.copy(twitterUsername=None)
    ).renderTiles
  }

  /** Renders one landscape product */
  def landscape = {
    ChoosePhotoDefaults().copy(products=List(sampleTile.copy(orientation=LandscapeOrientation))).renderTiles
  }


  /** Renders the choose photo page with a long bio */
  def longBio = {
    val longBio = (for(i <- 1 to 4) yield sampleBio).mkString("<br/><br/>")
    ChoosePhotoDefaults().copy(celeb = sampleCeleb.copy(bio=longBio)).renderTiles
  }

  /** Renders the carousel with n products, initially focusing on a particular product */
  def carousel(num: Int=3, focus: Int=1) = {
    val carouselProducts = for (i <- 1 to num) yield sampleCarouselProduct

    ChoosePhotoDefaults().copy(
      carouselProducts=carouselProducts,
      firstCarouselIndex=focus - 1
    ).renderCarousel
  }

  //
  // Private members
  //
  private[ChoosePhoto] case class ChoosePhotoDefaults(
    celeb: ChoosePhotoCelebrity = sampleCeleb,
    products: Iterable[ChoosePhotoTileProduct] = for (i <- 1 to 2) yield sampleTile,
    carouselProducts: Iterable[ChoosePhotoCarouselProduct] = for (i <- 1 to 2) yield sampleCarouselProduct,
    firstCarouselIndex: Int = 0,
    tiledViewLink: String="/Herp-Derpson/photos",
    recentEgraphs: Iterable[ChoosePhotoRecentEgraph] = for (i <- 1 to 1) yield sampleEgraph,
    partnerIcons: Iterable[ChoosePhotoPartnerIcon] = List(samplePartnerIcon, samplePartnerIcon)
  ) {
    def renderTiles = {
      views.html.frontend.celebrity_storefront_choose_photo_tiled(
        celeb, products, recentEgraphs, partnerIcons
      )
    }

    def renderCarousel = {
      views.html.frontend.celebrity_storefront_choose_photo_carousel(
        celeb, carouselProducts, firstCarouselIndex, tiledViewLink, recentEgraphs, partnerIcons
      )
    }
  }

  private def sampleCeleb = {
    ChoosePhotoCelebrity(
      name="Herp Derpson",
      profileUrl="http://placehold.it/80x100",
      organization="Major League Baseball",
      bio=sampleBio,
      roleDescription="Pitcher, Tampa Bay Rays",
      twitterUsername=Some("davidprice14")
    )
  }

  private def sampleEgraph = {
    ChoosePhotoRecentEgraph(
      productTitle="World Series 2011",
      ownersName="Tom Smith",
      imageUrl="http://placehold.it/340x200",
      url="/egraph/1"
    )
  }

  private def sampleTile = {
    ChoosePhotoTileProduct(
      name="2012 All-Star Game",
      price=BigDecimal(100.00).toMoney(),
      imageUrl="http://placehold.it/340x200",
      targetUrl="/Herp-Derpson/photos/2012-All-Star-Game",
      quantityRemaining=10,
      orientation=PortraitOrientation
    )
  }

  private def samplePartnerIcon = ChoosePhotoPartnerIcon(
    partnerName="Major League Baseball",
    imageUrl="http://placehold.it/100x50",
    link="/MLB"
  )

  private val sampleBio = {
    """Hoping to make his 3rd straight All-Star game appearance in 2012, David Price is one of the American League's most formidable pitchers. With 31 wins and a X.XX ERA over the past 2 seasons, Price's numbers stack up with just about any pitcher in the majors. He's off to a fast start this year – expect him to be leading the way for the Rays down the stretch come October.
      <br/><br/>
      Interesting fact: David Price is nearly inseparable with his French Bulldog, Astro. In fact, when the Rays decided to give away an action figure of Price, he insisted that they create one for Astro, too. Word. Oh, and in case you're wondering, yes, we are looking into whether or not Astro will create egraphs for his fans. We'll keep you posted."""
  }

  private val sampleCarouselProduct = {
    ChoosePhotoCarouselProduct(
      name="2011 All-Star Game",
      description="<strong>St. Petersburg, FL - 10/03/11</strong> David Price throws a pitch during game 7 of the 2008 ALCS against the Boston Red Sox. He threw the final four outs and earned his first career save in the game.",
      price=BigDecimal(100.00).toMoney(),
      imageUrl="http://placehold.it/575x400",
      personalizeLink="/2011-All-Star-Game/personalize",
      orientation=LandscapeOrientation,
      carouselUrl="this-product's-carousel-url",
      facebookShareLink="facebook-share-link",
      twitterShareLink = "www.twitter.com",
      carouselViewLink="www.egraphs.com",
      quantityRemaining=50
    )
  }
}

