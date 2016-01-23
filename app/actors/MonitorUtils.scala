package actors

import models.JsonFormats._
import models.MongoCollections._
import models.{Alert, Machine}
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.modules.reactivemongo.json._
import reactivemongo.api.Cursor
import services.MachineParkApiService

import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
 * Created by marianafranco on 23/01/16.
 */
trait MonitorUtils extends MachineParkApiService {

  private var machinesInAlert = Set[String]()

  def saveMachine(machine: Machine) = {
    machinesCollection.map(collection =>
      collection.insert(machine).onFailure {
        case e => Logger.error("An error has occurred when saving a machine: " + e.getMessage)
      })
  }

  def saveAlert(machine: Machine, avgCurrent: Double) = {
    val alert = new Alert(machine, avgCurrent)

    alertsCollection.map(collection =>
      collection.insert(alert).onFailure {
        case e => Logger.error("An error has occurred when saving an alert: " + e.printStackTrace)
      })
  }

  def createAlert(machine: Machine) = {
    // getting the current average for the last 5 mins
    val selector = Json.obj(
      "name" -> machine.name,
      "timestamp" -> Json.obj("$gt" -> machine.timestamp.minusMinutes(5))
    )

    machinesCollection.map { collection =>
      val cursor: Cursor[Machine] = collection.find(selector).cursor[Machine]()
      val machines: Future[List[Machine]] = cursor.collect[List]()

      machines.onComplete({
        case Success(list) =>
          val currents = list.map(_.current)
          val avgCurrent = currents.sum / currents.length
          saveAlert(machine, avgCurrent)

        case Failure(e) =>
          Logger.error("An error has occurred when getting machines by timestamp: " + e.getMessage)
      })
    }
  }

  def checkCurrent(machine: Machine) = {
    if (!machinesInAlert.contains(machine.name) && machine.current > machine.current_alert) {
      // send alert!
      machinesInAlert = machinesInAlert + machine.name
      createAlert(machine)
    } else if (machinesInAlert.contains(machine.name) && machine.current <= machine.current_alert) {
      machinesInAlert = machinesInAlert - machine.name // remove machine from the list of machines in alert state
    }
  }

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
