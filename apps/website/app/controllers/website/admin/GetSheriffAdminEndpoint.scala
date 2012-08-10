package controllers.website.admin

import models._
import play.mvc.Controller
import services.AppConfig
import services.blobs.Blobs
import services.http.{AdminRequestFilters, ControllerMethod}

/**
 * These are the Sheriff's tools to handle tasks that are not yet self-serve. If writing a one-time script, use "sheriff".
 */
private[controllers] trait GetSheriffAdminEndpoint {
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

  def getSheriffAdmin = controllerMethod() {
    adminFilters.requireAdministratorLogin {admin =>

      val action = params.get("action")
      action match {

        //
        // Write your one-time script here.
        //
        case "sheriff" => {
//          val order = orderStore.get(543)
//          order.copy(recipientName = "Ernesto J Pantoia").save()
        }

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

        case _ => "Not a valid action"
      }
    }
  }
}
