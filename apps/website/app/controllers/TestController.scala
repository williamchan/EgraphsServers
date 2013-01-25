package controllers

import play.api.mvc.Controller
import play.api.libs.concurrent.Promise
import play.api.libs.concurrent._
import play.api.mvc.Action
import services.mvc.celebrity.UpdateTwitterFollowersActor
import services.AppConfig
import models.CelebrityStore
import akka.pattern._
import services.mvc.celebrity.UpdateTwitterFollowersActor._
import services.mvc.celebrity.TwitterFollowersAgent
import akka.util.duration._
import services.db.DBSession
import services.db.TransactionSerializable
import models.Celebrity

object TestController extends Controller {

}