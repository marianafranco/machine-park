package controllers

import actors.WebSocketActor
import akka.actor.Props
import models.MongoCollections._
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController

/**
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController {

  def socket = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    Props(new WebSocketActor(out, machinesCollection, 20000 - 250))
  }

  def alertSocket = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    Props(new WebSocketActor(out, alertsCollection))
  }
}
