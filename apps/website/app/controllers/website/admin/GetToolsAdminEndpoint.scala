package controllers.website.admin

import models._
import models.enums._
import play.api.mvc.{Action, Controller}
import services.blobs.Blobs
import services.email.SiteShutdownEmail
import services.http.{WithDBConnection, ControllerMethod}
import services.http.filters.HttpFilters
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.db.{TransactionReadCommitted, DBSession, Schema}
import play.api.data._
import play.api.data.Forms._
import services.mvc.ImplicitHeaderAndFooterData
import models.Egraph
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent._
import services.cache.CacheFactory
import services.logging.Logging

/**
 * These are the Sheriff's tools to handle tasks that are not yet self-serve. If writing a one-time script, use "sheriff".
 */
private[controllers] trait GetToolsAdminEndpoint extends ImplicitHeaderAndFooterData {
  this: Controller =>
  import GetToolsAdminEndpoint._

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
  protected def cacheFactory: CacheFactory
  protected def enrollmentBatchStore: EnrollmentBatchStore
  protected def dbSession: DBSession
  val oneWeek = 25200
  //
  // Controllers
  //
  def getToolsAdmin = controllerMethod(WithDBConnection(readOnly=false)) {
    httpFilters.requireAdministratorLogin.inSession() { case (admin, adminAccount) =>
      Action { implicit request =>
        val action: String = Form("action" -> text).bindFromRequest.fold(formWithErrors => "", validForm => validForm)
        action match {
          case "" => Ok(views.html.Application.admin.admin_tools())
          //
          // Write your one-time script here.
          //

          case "sheriff" => {
            // orderStore.get(543).copy(recipientName = "Ernesto J Pantoia").save()
            val maybeCustomer = customerStore.findByEmail("david@egraphs.com")
            maybeCustomer.map( c =>
              cacheFactory.applicationCache.set[Boolean]("shutdown-email-" + c.id, false, oneWeek)
            )
            Ok
          }

          case "shutdown" => {
            // 2934 recipients
            for(i <- 0 to 293) {
              for(c <- customerStore.allRecipients.page(i *10, 10)) {
                scala.concurrent.Future {
                  if(c.isZipGenerated){
                    cacheFactory.applicationCache.get[Boolean]("shutdown-email-" + c.id) match  {
                      case Some(result) if(result == true) => println("Email already sent to customer " + c.id)
                      case _ => {
                        SiteShutdownEmail().send(
                          c.name,
                          //                    c.account.email,
                          c.account.email,
                          "https://s3.amazonaws.com/egraphs/" + c.zipFileBlobKey
                        )
                        cacheFactory.applicationCache.set[Boolean]("shutdown-email-" + c.id, true, oneWeek)
                      }
                    }
                  }
                }
              }
            }
           Ok("Sending some emails. Bye Bye :(")
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
            Ok(s)
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
              actors.EgraphActor.actor ! actors.ProcessEgraphMessage(egraphId = egraph.id, requestHeader = null)
            }
            Ok("I gave all Egraphs AwaitingVerification a kick.")
          }

          /**
           * Enrolls all celebrities that are stuck awaiting enrollment.
           */
          case "kick-pending-enrollmentbatches" => {
            for (pendingEnrollmentBatch <- enrollmentBatchStore.getEnrollmentBatchesPending()) {
              actors.EnrollmentBatchActor.actor ! actors.ProcessEnrollmentBatchMessage(id = pendingEnrollmentBatch.id)
            }
            Ok("I gave all pending EnrollmentBatches a kick")
          }

          case "generate-assets" => {
            val pageStart = request.getQueryString("pageStart").map(_.toInt).getOrElse(0)
            val pageLength = request.getQueryString("pageLength").map(_.toInt).getOrElse(10)
            customerStore.allRecipients.page(pageStart, pageLength).foreach { cust =>
              scala.concurrent.future {
                if (!(cust.isZipGenerated || cust.id == 7) /* Ignore J. Cohn, employee */ ) {
                  val (_, timing) = services.Time.stopwatch {
                    cust.writeZipFile()
                  }

                  log(s"ZIP cust=${cust.id}: Finished in $timing seconds.")
                } else {
                  log(s"ZIP cust=${cust.id}: Skipping ZIP file generation: Already generated")
                }

                log(s"ZIP cust=${cust.id}: ZIP available at https://s3.amazonaws.com/egraphs/${cust.zipFileBlobKey}")
              }
            }

            Ok("Generated assets for customers")
          }

          //
          // Keep the rest of these actions commented out. With great power comes great responsibility...
          // at least until these actions are made self-serve for the Operations team.
          //

//          case "create-admin" => {
//            val adminEmail = "will@egraphs.com"
//            val admin = administratorStore.findByEmail(adminEmail)
//            if (admin.isEmpty) {
//              var account = accountStore.findByEmail(adminEmail)
//              if (account.isEmpty) {
//                account = Some(Account(email = adminEmail).save())
//              }
//              val administrator = Administrator().save()
//              account.get.copy(administratorId = Some(administrator.id)).save()
//            }
//            Ok("Admin created")
//          }
//          case "check-enrollmentbatch-status" => {
//            val celebrity = celebrityStore.get(Form("celebrityId" -> longNumber).bindFromRequest.get)
//            enrollmentBatchStore.getOpenEnrollmentBatch(celebrity) match {
//              case None => Ok("Celebrity does not have any unused enrollment batches.")
//              case Some(batch) => Ok("Celebrity has an enrollmentbatch #" + batch.id + " with " +
//                enrollmentBatchStore.countEnrollmentSamples(batch.id) + " enrollment samples.")
//            }
//          }
//          /**
//           * Before enrollment can be attempted using an enrollment batch, the batch must be marked as complete.
//           */
//          case "mark-enrollmentbatch-complete" => {
//            val enrollmentBatchId = Form("enrollmentBatchId" -> longNumber).bindFromRequest.get
//            enrollmentBatchStore.get(enrollmentBatchId).copy(isBatchComplete = true).save()
//            Ok("Enrollment batched marked as complete")
//          }
          case _ => Ok("Not a valid action")
        }
      }
    }
  }
}

object GetToolsAdminEndpoint extends Logging
