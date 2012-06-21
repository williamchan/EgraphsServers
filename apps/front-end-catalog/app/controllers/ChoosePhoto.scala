package controllers

import play.mvc.Controller

import models.frontend.storefront._
import java.text.SimpleDateFormat
import models.frontend.storefront.ChoosePhotoProductTile
import models.frontend.storefront.ChoosePhotoRecentEgraph
import models.frontend.storefront.ChoosePhotoCelebrity

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller {
  import frontend.formatting.MoneyFormatting.Conversions._

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def withProducts(num: Int = 1) = {
    val products = for(i <- 1 to num) yield sampleTile
    RenderTiles().copy(products=products).render
  }

  def withRecentEgraphs(num: Int = 1) = {
    val recentEgraphs = for (i <- 1 to num) yield sampleEgraph
    RenderTiles().copy(recentEgraphs=recentEgraphs).render
  }

  def withPartnerIcons(num: Int=1) = {
    val icons = for (i <- 1 to num) yield samplePartnerIcon
    RenderTiles().copy(partnerIcons=icons).render
  }

  def landscape = {
    RenderTiles().copy(products=List(sampleTile.copy(orientation=LandscapeOrientation))).render
  }

  def longBio = {
    val longBio = (for(i <- 1 to 4) yield sampleBio).mkString("<br/><br/>")
    RenderTiles().copy(celeb = sampleCeleb.copy(bio=longBio)).render
  }

  private[ChoosePhoto] case class RenderTiles(
    celeb: ChoosePhotoCelebrity = sampleCeleb,
    products: Iterable[ChoosePhotoProductTile] = for (i <- 1 to 2) yield sampleTile,
    recentEgraphs: Iterable[ChoosePhotoRecentEgraph] = for (i <- 1 to 1) yield sampleEgraph,
    partnerIcons: Iterable[ChoosePhotoPartnerIcon] = List(samplePartnerIcon, samplePartnerIcon)
  ) {
    def render = {
      views.frontend.html.celebrity_storefront_choose_photo_tiled(
        celeb, products, recentEgraphs, partnerIcons
      )
    }
  }

  private def sampleCeleb = {
    ChoosePhotoCelebrity(
      name="Herp Derpson",
      profileUrl="http://placehold.it/80x100",
      category="Major League Baseball",
      bio=sampleBio,
      categoryRole="Pitcher, Tampa Bay Rays",
      twitterUsername="davidprice14",
      quantityAvailable=10,
      deliveryDate=dateFormat.parse("2012-07-13")
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

  def carousel = {
    views.frontend.html.celebrity_storefront_choose_photo_carousel()
  }

  private def sampleTile = {
    ChoosePhotoProductTile(
      name="2012 All-Star Game",
      price=BigDecimal(100.00).toMoney(),
      imageUrl="http://placehold.it/340x200",
      targetUrl="/Herp-Derpson/photos/2012-All-Star-Game",
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
}

