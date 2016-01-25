package controllers

import actors.WebSocketActor
import akka.actor.Props
import models.MongoCollections._
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController

/**
 * The application's websockets and REST APIs used by the UI.
 *
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController {

  /**
   * Machine status websocket.
   * @return all machine's status saved in the db.
   */
  def socket = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    Props(new WebSocketActor(out, machinesCollection, 20000 - 250))
  }

  /**
   * Alerts websocket.
   * @return all alerts saved in the db
   */
  def alertSocket = WebSocket.acceptWithActor[String, JsValue] { request => out =>
    Props(new WebSocketActor(out, alertsCollection))
  }
}
