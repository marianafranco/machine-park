package controllers

import actors.WebSocketActor
import akka.actor.Props
import models.Correlation
import models.JsonFormats._
import models.MongoCollections._
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController

import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.Try

/**
 * The application's websockets and REST APIs used by the UI.
 *
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController with CorrelationCalculation {

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

  /**
   * Returns the correlation between the machine's current and the env variables.
   * @param machineName
   * @return the correlation.
   */
  def getMachineCorrelation(machineName: String) = Action.async { request =>

    val machineEnv = getMachineEnvs(machineName)

    machineEnv.map { list =>
      if (list.nonEmpty) {

        val correlations = Try {
          correlationsCalculation(list)
        }.recover {
          case x => Correlation(0, 0, 0)
        }.get
        Ok(Json.toJson(correlations))
      } else {
        NotFound("Machine Correlation Not found")
      }
    }

  }

}
