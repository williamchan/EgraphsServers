package controllers

import play.mvc.Controller

import models.frontend.storefront.{ChoosePhotoCelebrity, PortraitOrientation, ChoosePhotoProductTile, ChoosePhotoRecentEgraph}
import java.text.SimpleDateFormat

/**
 * Permutations of the Celebrity Storefront: Choose Photo, tiled view.
 */
object ChoosePhoto extends Controller {
  import frontend.formatting.MoneyFormatting.Conversions._

  val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def index = {
    views.frontend.html.celebrity_storefront_choose_photo_tiled(
      sampleCeleb,
      for (i <- 1 to 2) yield sampleTile,
      for (i <- 1 to 1) yield sampleEgraph,
      List(icon)
    )
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
      targetUrl="#",
      orientation=PortraitOrientation
    )
  }

  private def icon = "http://placehold.it/100x50"

  private val sampleBio = {
    """Hoping to make his 3rd straight All-Star game appearance in 2012, David Price is one of the American League's most formidable pitchers. With 31 wins and a X.XX ERA over the past 2 seasons, Price's numbers stack up with just about any pitcher in the majors. He's off to a fast start this year – expect him to be leading the way for the Rays down the stretch come October.
      <br/><br/>
      Interesting fact: David Price is nearly inseparable with his French Bulldog, Astro. In fact, when the Rays decided to give away an action figure of Price, he insisted that they create one for Astro, too. Word. Oh, and in case you're wondering, yes, we are looking into whether or not Astro will create egraphs for his fans. We'll keep you posted."""
  }
}

