package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.blobs.{AccessPolicy, Blobs}
import services.db.Schema
import models._
import enums.EgraphState
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.report._
import play.mvc.results.RenderBinary

private[controllers] trait GetScriptAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  protected def blobs = AppConfig.instance[Blobs]
  protected def schema = AppConfig.instance[Schema]
  protected def accountStore = AppConfig.instance[AccountStore]
  protected def administratorStore = AppConfig.instance[AdministratorStore]
  protected def celebrityStore = AppConfig.instance[CelebrityStore]
  protected def customerStore = AppConfig.instance[CustomerStore]
  protected def egraphStore = AppConfig.instance[EgraphStore]
  protected def enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]
  protected def inventoryBatchStore = AppConfig.instance[InventoryBatchStore]
  protected def productStore = AppConfig.instance[ProductStore]
  protected def orderStore = AppConfig.instance[OrderStore]

  def getScriptAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {admin =>

      val action = params.get("action")
      action match {
        case "gc" => {
          Runtime.getRuntime.gc()
        }

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

        case "create-admin" => {
          val adminEmail = params.get("admin-email")
          val admin = administratorStore.findByEmail(adminEmail)
          if (admin.isEmpty) {
            var account = accountStore.findByEmail(adminEmail)
            if (account.isEmpty) {
              account = Some(Account(email = adminEmail).save())
            }
            val administrator = Administrator().save()
            account.get.copy(administratorId = Some(administrator.id)).save()
          }
          "Admin created"
        }

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

        case "kick-pending-enrollmentbatches" => {
          for (pendingEnrollmentBatch <- enrollmentBatchStore.getEnrollmentBatchesPending()) {
            actors.EnrollmentBatchActor.actor ! actors.ProcessEnrollmentBatchMessage(id = pendingEnrollmentBatch.id)
          }
          "I gave all pending EnrollmentBatches a kick"
        }

        case "kick-egraph" => {
          val egraphId = params.get("egraphId")
          val egraph = egraphStore.findById(id = egraphId.toLong)
          if (egraph.isDefined && egraph.get.egraphState == EgraphState.AwaitingVerification) {
            actors.EgraphActor.actor ! actors.ProcessEgraphMessage(id = egraph.get.id)
          }
          "I gave that Egraph a kick."
        }

        case "kick-enrollmentbatch" => {
          val batchId = params.get("batchId")
          val enrollmentBatch = enrollmentBatchStore.findById(id = batchId.toLong)
          if (enrollmentBatch.isDefined) {
            actors.EnrollmentBatchActor.actor ! actors.ProcessEnrollmentBatchMessage(id = enrollmentBatch.get.id)
          }
          "I gave that EnrollmentBatch a kick"
        }

        case "email-list-report" => {
          val report = new EmailListReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "inventory-batch-report" => {
          val report = new InventoryBatchReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "order-report" => {
          val report = new OrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        case "physical-print-report" => {
          val report = new PrintOrderReport(schema).report()
          new RenderBinary(report, report.getName)
        }

        // Will be moved into PrintOrder functionality.
        case "generate-large-egraph" => {
          val width = 2446 // width from Feeny
          val egraphId = params.get("egraphId").toLong
          val egraph = egraphStore.get(egraphId)
          val order = egraph.order
          val product = order.product
          val rawSignedImage = egraph.image(product.photoImage)
          val image = rawSignedImage
            .withSigningOriginOffset(product.signingOriginX.toDouble, product.signingOriginY.toDouble)
            .scaledToWidth(width)
          image.rasterized.getSavedUrl(AccessPolicy.Public)
        }

        case _ => "Not a valid action"
      }
    }
  }
}
