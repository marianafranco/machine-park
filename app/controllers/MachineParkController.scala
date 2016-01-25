package controllers

import actors.WebSocketActor
import akka.actor.Props
import models.{Correlation, MachineEnv}
import models.MongoCollections._
import play.api.Play.current
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json._

import scala.concurrent.ExecutionContext.Implicits.global
import models.JsonFormats._

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

  def getMachineCorrelation(machineName: String) = Action.async { request =>
    val machineEnv = getMachineEnvs(machineName)

    machineEnv.map { list =>
      val tuple: List[(Double, Double, Double, Double)] = list.map(x => (x.current, x.temperature, x.pressure, x.humidity))

      val current: List[Double] = tuple.map(x => x._1)
      val temperature = tuple.map(x => x._2)
      val pressure = tuple.map(x => x._3)
      val humidity = tuple.map(x => x._4)

      val correlationTemperature = calculateCorrelation(current, temperature)
      val correlationPressure = calculateCorrelation(current, pressure)
      val correlationHumidity = calculateCorrelation(current, humidity)

      val correlation = Correlation(correlationTemperature, correlationPressure, correlationHumidity)
      Ok(Json.toJson(correlation))
    }
  }

  def getMachineEnvs(machineName: String) = {
    val selector = Json.obj(
      "name" -> machineName
    )
    envMachinesCollection.find(selector).cursor[MachineEnv]().collect[List]()
  }

  def calculateCorrelation(var1: List[Double], var2: List[Double]) = {
    val meanVar1 = var1.sum / var1.length
    val meanVar2 = var2.sum / var2.length

    val diffVar1 = var1.map(x => x - meanVar1)
    val diffVar2 = var2.map(x => x - meanVar2)

    val stdDevVar1 = Math.sqrt(diffVar1.map(x => x*x).sum)
    val stdDevVar2 = Math.sqrt(diffVar2.map(x => x*x).sum)

    val pair = diffVar1 zip diffVar2

    val covariance = pair.map(x => x._1 * x._2).sum

    covariance / (stdDevVar1 * stdDevVar2)
  }
}
