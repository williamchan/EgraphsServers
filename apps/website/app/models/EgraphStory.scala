package models

import com.google.inject.Inject
import controllers.website.consumer.{StorefrontChoosePhotoConsumerEndpoints, CelebrityLandingConsumerEndpoint}
import egraphs.playutils.Enum
import java.sql.Timestamp
import java.text.SimpleDateFormat
import org.apache.commons.lang3.StringEscapeUtils._
import services.{TemplateEngine, AppConfig}

/**
 * Represents the story of an egraph, as presented on the egraph page.
 *
 * @param titleTemplate title template as specified on the [[models.Product]]
 * @param bodyTemplate title template as specified on the [[models.bodyTemplate]]
 * @param celebName the celebrity's public name
 * @param celebUrlSlug see [[models.Celebrity.urlSlug]]
 * @param recipientName name of the [[models.Customer]] receiving the egraph.
 * @param productName name of the purchased [[models.Product]]
 * @param productUrlSlug see [[models.Product.urlSlug]]
 * @param orderTimestamp the moment the buying [[models.Customer]] ordered the [[models.Product]]
 * @param signingTimestamp the moment the [[models.Celebrity]] fulfilled the [[models.Order]]
 * @param services Services needed for the EgraphStory to manipulate its data properly.
 */
case class EgraphStory(
  private val titleTemplate: String,
  private val bodyTemplate: String,
  private val celebName: String,
  private val celebUrlSlug: String,
  private val recipientName: String,
  private val productName: String,
  private val productUrlSlug: String,
  private val orderTimestamp: Timestamp,
  private val signingTimestamp: Timestamp,
  private val services: EgraphStoryServices = AppConfig.instance[EgraphStoryServices]
) {

  //
  // Public methods
  //
  /** Returns the story title */
  def title: String = {
    services.templateEngine.evaluate(escapeHtml4(titleTemplate), templateParams)
  }

  /** Returns the body of the story */
  def body: String = {
    services.templateEngine.evaluate(escapeHtml4(bodyTemplate), templateParams)
  }

  //
  // Private methods
  //
  private val templateParams: Map[String, String] = {
    import EgraphStoryField._
    val pairs = for (templateField <- EgraphStoryField.values) yield {
      val paramValue = templateField match {
        case CelebrityName => celebName
        case StartCelebrityLink => startCelebPageLink
        case RecipientName => recipientName
        case ProductName => productName
        case StartProductLink => startProductLink
        case DateOrdered => formatTimestamp(orderTimestamp)
        case DateSigned => formatTimestamp(signingTimestamp)
        case FinishLink => "</a>"
        case _ => throw new IllegalArgumentException("Template param not recognized")
      }

      (templateField.name, paramValue)
    }

    pairs.toMap
  }

  private def dateFormat = {
    new SimpleDateFormat("MMMM dd, yyyy")
  }

  private def startCelebPageLink: String = {
    htmlAnchorStart(href=CelebrityLandingConsumerEndpoint.url(celebUrlSlug).url)
  }

  private def startProductLink: String = {
    htmlAnchorStart(
      href=StorefrontChoosePhotoConsumerEndpoints.url(celebUrlSlug, productUrlSlug).url
    )
  }

  private def htmlAnchorStart(href: String) = {
    "<a href='" + href + "' >"
  }

  private def formatTimestamp(timestamp: Timestamp): String = {
    dateFormat.format(timestamp)
  }
}

/**
 * Service interfaces used by the EgraphStory.
 *
 * @param templateEngine the templating engine used to provide user-side templating.
 */
case class EgraphStoryServices @Inject() (templateEngine: TemplateEngine)

/**
 * The set of fields available for users to address when writing their stories and
 * story titles. This permits them to provide the following type of narratives for the
 * story, which will get interpreted on the fly by the Egraph page.
 *
 * {{{
 *  Everybody wants a piece of {start-celebrity-link}{signer-name}{end-link}'s fame.
 *  But only {recipient-name} got it. Because of Egraphs.
 * }}}
 *
 */
object EgraphStoryField extends Enum {
  sealed trait EnumVal extends Value

  /** Public name of the celebrity */
  val CelebrityName = new EnumVal { val name = "signer_name" }

  /**
   * Begins a link to the celebrity's page. Must be closed by an
   * [[models.EgraphStoryField.FinishLink]]
   * */
  val StartCelebrityLink = new EnumVal { val name = "signer_link"}

  /** Name of the person receiving the egraph */
  val RecipientName = new EnumVal { val name = "recipient_name"}

  /** Name of the product being sold */
  val ProductName = new EnumVal { val name = "product_name"}

  /**
   * Begins a link to the photographic product. Must be closed by a
   * [[models.EgraphStoryField.FinishLink]]
   **/
  val StartProductLink = new EnumVal { val name="product_link"}

  /** Prints the date the Egraph was ordered. */
  val DateOrdered = new EnumVal { val name = "date_ordered"}

  /** Prints the date the Egraph was signed */
  val DateSigned = new EnumVal { val name = "date_signed"}

  /** Closes the last opened link */
  val FinishLink = new EnumVal { val name="end_link" }
}
