package controllers.website.admin

import play.mvc.Controller
import services.http.{AdminRequestFilters, ControllerMethod}
import services.blobs.Blobs
import services.db.Schema
import models._
import enums.EgraphState
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.{Dimensions, AppConfig}

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

        case "reset-signingScale" => {
          val products: Query[(Product)] = from(schema.products)((p) => select(p))
          for (product <- products) {
            val image = product.photoImage
            val imageDimensions = Dimensions(image.getWidth, image.getHeight)
            val signingScaleDimensions = if (imageDimensions.isLandscape) Product.defaultLandscapeSigningScale else Product.defaultPortraitSigningScale
            product.copy(signingScaleH = signingScaleDimensions.height, signingScaleW = signingScaleDimensions.width).save()
          }
          "signingScaleW and signingScaleH set for all products"
        }

        case _ => "Not a valid action"
      }
    }
  }
}
