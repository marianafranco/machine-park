package actors

import models.JsonFormats._
import models.MongoCollections._
import models.{MachineEnv, Environment, Alert, Machine}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import services.MachineParkApiService

import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
 * Utility methods for the MonitorActor and the CorrelationActor.
 * 
 * Created by marianafranco on 23/01/16.
 */
trait MonitorUtils extends MachineParkApiService {

  // current list of machines in alert
  private var machinesInAlert = Set[String]()

  /**
   * Save a machine status in the db.
   * @param machine
   */
  def saveMachine(machine: Machine) = {
    machinesCollection.map(collection =>
      collection.insert(machine).onFailure {
        case e => Logger.error("An error has occurred when saving a machine: " + e.printStackTrace)
      })
  }

  /**
   * Save the machines current with the environmental data (temperature, pressure, humidity) in the db.
   * @param machine
   * @param env
   */
  def saveMachineEnv(machine: Machine, env: Environment) = {
    val machineEnv = new MachineEnv(machine, env)
    envMachinesCollection.insert(machineEnv).onFailure {
      case e => Logger.error("An error has occurred when saving an machine env: " + e.printStackTrace)
    }
  }

  /**
   * Save a new alert in the db.
   * @param machine
   * @param avgCurrent
   */
  def saveAlert(machine: Machine, avgCurrent: Double) = {
    val alert = new Alert(machine, avgCurrent)

    alertsCollection.map(collection =>
      collection.insert(alert).onFailure {
        case e => Logger.error("An error has occurred when saving an alert: " + e.printStackTrace)
      })
  }

  /**
   * Calculate the average current drew by a machine in the last 5 minutes and creates an alert.
   * @param machine
   */
  def createAlert(machine: Machine) = {
    // getting the average current  for the last 5 mins
    val selector = Json.obj(
      "name" -> machine.name,
      "timestamp" -> Json.obj("$gt" -> machine.timestamp.minusMinutes(5))
    )

    machinesCollection.map { collection =>
      val cursor: Cursor[Machine] = collection.find(selector).cursor[Machine]()
      val machines: Future[List[Machine]] = cursor.collect[List]()

      machines.onComplete({
        case Success(list) =>
          if (!list.isEmpty) {
            val currents = list.map(_.current)
            val avgCurrent = (currents.sum + machine.current) / (currents.length + 1)
            saveAlert(machine, avgCurrent)
          }

        case Failure(e) =>
          Logger.error("An error has occurred when getting machines by timestamp: " + e.getMessage)
      })
    }
  }

  /**
   * Check if the machine's current is above the threshold. In positive case, create a new alert.
   * @param machine
   */
  def checkCurrent(machine: Machine) = {
    if (!machinesInAlert.contains(machine.name) && machine.current > machine.current_alert) {
      // send alert!
      machinesInAlert = machinesInAlert + machine.name
      createAlert(machine)
    } else if (machinesInAlert.contains(machine.name) && machine.current <= machine.current_alert) {
      // removing the machine from the list of machines in alert state
      machinesInAlert = machinesInAlert - machine.name
    }
  }

  /**
   * Get the machine status, check if the current is above the threshold, and save the data in the db.
   * @param url the machine's URL
   */
  def retrieveMachineCurrent(url: String): Unit = {
    val mFutureResponse = getMachineInfo(url)

    mFutureResponse onComplete {
      case Success(machine) =>
        Logger.debug(machine.name)
        checkCurrent(machine)
        saveMachine(machine)

      case Failure(t) =>
        Logger.error("An error has occurred while getting the machines current: " + t.getMessage)
    }
  }
}
