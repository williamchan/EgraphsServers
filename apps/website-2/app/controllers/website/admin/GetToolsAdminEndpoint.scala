package controllers.website.admin

import models._
import models.enums._
import play.api.mvc.{Action, Controller}
import services.AppConfig
import services.blobs.Blobs
import services.http.{WithDBConnection, ControllerMethod}
import services.http.filters.HttpFilters
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.db.Schema
import services.http.SafePlayParams.Conversions._
import play.api.data._
import play.api.data.Forms._
import services.mvc.ImplicitHeaderAndFooterData

/**
 * These are the Sheriff's tools to handle tasks that are not yet self-serve. If writing a one-time script, use "sheriff".
 */
private[controllers] trait GetToolsAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>

  //
  // Injected services
  //
  protected def controllerMethod: ControllerMethod
  protected def httpFilters: HttpFilters
  protected def blobs: Blobs
  protected def accountStore: AccountStore
  protected def administratorStore: AdministratorStore
  protected def celebrityStore: CelebrityStore
  protected def customerStore: CustomerStore
  protected def egraphStore: EgraphStore
  protected def inventoryBatchStore: InventoryBatchStore
  protected def productStore: ProductStore
  protected def orderStore: OrderStore
  protected def schema: Schema
  protected def enrollmentBatchStore: EnrollmentBatchStore

  //
  // Controllers
  //
  def getToolsAdmin = controllerMethod(WithDBConnection(readOnly=false)) {
    httpFilters.requireAdministratorLogin.inSession() { (admin, adminAccount) =>
      Action { implicit request =>
        val action: String = Form("action" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        action match {
          case "" => Ok(views.html.Application.admin.admin_tools())
          //
          // Write your one-time script here.
          //
          case "sheriff" => {
            // val order = orderStore.get(543)
            // order.copy(recipientName = "Ernesto J Pantoia").save()
            Ok
          }

//          /**
//           * Prints basic parameters about the JVM the instance is running on
//           */
//          case "print-jvm-params" => {
//            import management.ManagementFactory
//            import scala.collection.JavaConversions._
//
//            var s = ""
//            for (z <- ManagementFactory.getMemoryPoolMXBeans) {
//              if (z.getName.contains("Perm Gen")) {
//                s += (z.getName + " " + z.getUsage.getUsed + " (max: " + z.getUsage.getMax + ").  ")
//              }
//            }
//            s += " Runtime.getRuntime.maxMemory() = " + Runtime.getRuntime.maxMemory() + "."
//            s
//            Ok
//          }

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
              actors.EgraphActor.actor ! actors.ProcessEgraphMessage(egraphId = egraph.id, requestHeader = null)
            }
            "I gave all Egraphs AwaitingVerification a kick."
            Ok
          }

          /**
           * Enrolls all celebrities that are stuck awaiting enrollment.
           */
          case "kick-pending-enrollmentbatches" => {
            for (pendingEnrollmentBatch <- enrollmentBatchStore.getEnrollmentBatchesPending()) {
              actors.EnrollmentBatchActor.actor ! actors.ProcessEnrollmentBatchMessage(id = pendingEnrollmentBatch.id)
            }
            "I gave all pending EnrollmentBatches a kick"
            Ok
          }

//          case "check-enrollmentbatch-status" => {
//            val maybeCelebrityId = params.getLongOption("celebrityId")
//            maybeCelebrityId.map {celebrityId =>
//              val celebrity = celebrityStore.get(celebrityId)
//              enrollmentBatchStore.getOpenEnrollmentBatch(celebrity) match {
//                case None => "Celebrity with id " + celebrityId + " does not have any unused enrollment batches."
//                case Some(batch) => "Celebrity has an enrollmentbatch #" + batch.id + " with " +
//                  enrollmentBatchStore.countEnrollmentSamples(batch.id) + " enrollment samples."
//              }
//            }.getOrElse("")
//          }

          //
          // Keep the rest of these actions commented out. With great power comes great responsibility...
          // at least until these actions are made self-serve for the Operations team.
          //

//          case "generate-large-egraph" =>
//            import services.http.SafePlayParams.Conversions._
//            val orderStore = AppConfig.instance[OrderStore]
//            val errorOrBlobUrl = for (
//              orderId <- params.getLongOption("orderId").toRight("orderId param required").right;
//              fulfilledOrder <- orderStore
//                                  .findFulfilledWithId(orderId)
//                                  .toRight("No fulfilled order with ID" + orderId + "found")
//                                  .right
//            ) yield {
//              val FulfilledOrder(order, egraph) = fulfilledOrder
//              val product = order.product
//              val productPhoto = product.photoImage
//              val targetWidth = {
//                val masterWidth = productPhoto.getWidth
//                if (masterWidth < PrintOrder.defaultPngWidth) masterWidth else PrintOrder.defaultPngWidth
//              }
//
//              egraph.image(productPhoto)
//                .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
//                .scaledToWidth(targetWidth)
//                .rasterized
//                .saveAndGetUrl(services.blobs.AccessPolicy.Public)
//            }
//
//            errorOrBlobUrl.fold(error => error, url => url)

//          case "unapprove-orders" => {
//            val orderIds = List[Long]()
//            for (orderId <- orderIds) {
//              val order = orderStore.get(orderId)
//              order.withReviewStatus(OrderReviewStatus.PendingAdminReview).save()
//            }
//            "Orders have been reset to reviewStatus = 'PendingAdminReview'"
//          }
//          case "create-admin" => {
//            val adminEmail = params.get("admin-email")
//            val admin = administratorStore.findByEmail(adminEmail)
//            if (admin.isEmpty) {
//              var account = accountStore.findByEmail(adminEmail)
//              if (account.isEmpty) {
//                account = Some(Account(email = adminEmail).save())
//              }
//              val administrator = Administrator().save()
//              account.get.copy(administratorId = Some(administrator.id)).save()
//            }
//            "Admin created"
//          }
//          /**
//           * Before enrollment can be attempted using an enrollment batch, the batch must be marked as complete.
//           */
//          case "mark-enrollmentbatch-complete" => {
//            val enrollmentBatchId = params.get("enrollmentBatchId").toLong
//            enrollmentBatchStore.get(enrollmentBatchId).copy(isBatchComplete = true).save()
//          }
          case _ => Ok("Not a valid action")
        }
      }
    }
  }
}
