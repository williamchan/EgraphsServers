package controllers.website.admin

import play.mvc.Controller
import models._
import models.enums.EgraphState
import org.squeryl.Query
import org.squeryl.PrimitiveTypeMode._
import services.AppConfig
import services.db.Schema
import services.http.{AdminRequestFilters, ControllerMethod}

/**
 * These actions can be executed by any admin.
 */
private[controllers] trait GetScriptAdminEndpoint {
  this: Controller =>

  protected def controllerMethod: ControllerMethod
  protected def adminFilters: AdminRequestFilters
  private lazy val schema = AppConfig.instance[Schema]
  private lazy val enrollmentBatchStore = AppConfig.instance[EnrollmentBatchStore]

  def getScriptAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {admin =>

      val action = params.get("action")
      action match {
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
    }
  }
}
