package controllers.website.admin

import models._
import enums.EgraphState
import play.mvc.Controller
import services.AppConfig
import services.blobs.Blobs
import services.http.{AdminRequestFilters, ControllerMethod}
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import models.Egraph
import services.db.Schema

/**
 * These are the Sheriff's tools to handle tasks that are not yet self-serve. If writing a one-time script, use "sheriff".
 */
private[controllers] trait GetToolsAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def blobs = AppConfig.instance[Blobs]
  protected def accountStore = AppConfig.instance[AccountStore]
  protected def administratorStore = AppConfig.instance[AdministratorStore]
  protected def celebrityStore = AppConfig.instance[CelebrityStore]
  protected def customerStore = AppConfig.instance[CustomerStore]
  protected def egraphStore = AppConfig.instance[EgraphStore]
  protected def inventoryBatchStore = AppConfig.instance[InventoryBatchStore]
  protected def productStore = AppConfig.instance[ProductStore]
  protected def orderStore = AppConfig.instance[OrderStore]
  private lazy val schema = AppConfig.instance[Schema]
  private lazy val enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]

  def getSheriffAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {admin =>

      val actionOption = Option(params.get("action"))

      actionOption match {
        case Some(action) => action match {
          //
          // Write your one-time script here.
          //
          case "sheriff" => {
  //          val order = orderStore.get(543)
  //          order.copy(recipientName = "Ernesto J Pantoia").save()
          }

          /**
           * Prints basic parameters about the JVM the instance is running on
           */
          case "print-jvm-params" => {
            import management.ManagementFactory
            import scala.collection.JavaConversions._

            var s = ""
            for (z <- ManagementFactory.getMemoryPoolMXBeans) {
              if (z.getName.contains("Perm Gen")) {
                s += (z.getName + " " + z.getUsage.getUsed + " (max: " + z.getUsage.getMax + ").  ")
              }
            }
            s += " Runtime.getRuntime.maxMemory() = " + Runtime.getRuntime.maxMemory() + "."
            s
          }

          /**
           * Processes all egraphs that have been posted by a celeb but not verified by biometrics.
           * Pushes the egraphs to biometrics, in order to get to the admin flow.
           */
          case "kick-egraphs-awaitingverification" => {
            // find all Egraphs that are AwaitingVerification and give them a kick...
            val egraphsAwaitingVerification: Query[(Egraph)] = from(schema.egraphs)(
              (e) => where(e._egraphState === EgraphState.AwaitingVerification.name) select (e)
            )
            for (egraph <- egraphsAwaitingVerification) {
              actors.EgraphActor.actor ! actors.ProcessEgraphMessage(id = egraph.id)
            }
            "I gave all Egraphs AwaitingVerification a kick."
          }

          /**
           * Enrolls all celebrities that are stuck awaiting enrollment.
           */
          case "kick-pending-enrollmentbatches" => {
            for (pendingEnrollmentBatch <- enrollmentBatchStore.getEnrollmentBatchesPending()) {
              actors.EnrollmentBatchActor.actor ! actors.ProcessEnrollmentBatchMessage(id = pendingEnrollmentBatch.id)
            }
            "I gave all pending EnrollmentBatches a kick"
          }

          case _ => "Not a valid action"
        }
        case _ => views.Application.admin.html.admin_tools()
      }
    }
  }
}

// Will be moved into PrintOrder functionality.
//        case "generate-large-egraph" => {
//          val width = 2446 // width from Feeny
//          val egraphId = params.get("egraphId").toLong
//          val egraph = egraphStore.get(egraphId)
//          val order = egraph.order
//          val product = order.product
//          val rawSignedImage = egraph.image(product.photoImage)
//          val image = rawSignedImage
//            .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
//            .scaledToWidth(width)
//          image.rasterized.getSavedUrl(AccessPolicy.Public) // also returns URL
//        }

//
// Keep the rest of these actions commented out. With great power comes great responsibility...
// at least until these actions are made self-serve for the Operations team.
//

//        case "unapprove-orders" => {
//          val orderIds = List[Long]()
//          for (orderId <- orderIds) {
//            val order = orderStore.get(orderId)
//            order.withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
//          }
//          "Orders have been reset to reviewStatus = 'PendingAdminReview'"
//        }

//        case "create-admin" => {
//          val adminEmail = params.get("admin-email")
//          val admin = administratorStore.findByEmail(adminEmail)
//          if (admin.isEmpty) {
//            var account = accountStore.findByEmail(adminEmail)
//            if (account.isEmpty) {
//              account = Some(Account(email = adminEmail).save())
//            }
//            val administrator = Administrator().save()
//            account.get.copy(administratorId = Some(administrator.id)).save()
//          }
//          "Admin created"
//        }