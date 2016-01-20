package actors

import akka.actor.{Actor, Cancellable}
import models.{Alert, Machine}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.json._
import reactivemongo.api.{Cursor}
import services.MachineParkApiService
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/**
 * Created by marianafranco on 25/11/15.
 */
class MonitorActor extends Actor with MachineParkApiService {

  private val MONITOR = "MONITOR"

  private var scheduler: Cancellable = _

  private var machines  = List[String]()

  private var machinesInAlert  = Set[String]()

  override def preStart(): Unit = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,

      interval = 5 seconds,
      receiver = self,
      message = MONITOR
    )
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def saveMachine(machine: Machine) = {
    import models.MongoModel._
    import models.JsonFormats._

    machinesCollection.map(collection =>
      collection.insert(machine).onComplete {
        case Failure(e) => Logger.error("An error has occurred when saving a machine: " + e.getMessage)
        case Success(_) =>
      })
  }

  def updateMachine(machine: Machine) = {
    import models.MongoModel._
    import models.JsonFormats._

    val selector = Json.obj("name" -> machine.name)

    machinesCollection.map(collection =>
      collection.update(selector, machine, upsert = true).onComplete {
        case Failure(e) => Logger.error("An error has occurred when updating a machine: " + e.getMessage)
        case Success(_) =>
      })
  }

  def saveAlert(machine: Machine, avgCurrent: Double) = {
    import models.MongoModel._
    import models.JsonFormats._

    val alert = new Alert(machine, avgCurrent)

    alertsCollection.map(collection =>
      collection.insert(alert).onComplete {
        case Failure(e) => Logger.error("An error has occurred when saving an alert: " + e.getMessage)
        case Success(_) =>
      })
  }

  def createAlert(machine: Machine) = {
    import models.MongoModel._
    import models.JsonFormats._

    // getting the current average for the last 5 mins
    val selector = Json.obj(
      "name" -> machine.name,
      "timestamp" -> Json.obj("$gt" ->  machine.timestamp.minusMinutes(5))
    )

    machinesCollection.map { collection =>
      val cursor: Cursor[Machine] = collection.find(selector).cursor[Machine]()
      val machines: Future[List[Machine]] = cursor.collect[List]()

      machines.onComplete({
        case Failure(e) => Logger.error("An error has occurred when getting machines by timestamp: " + e.getMessage)
        case Success(list) =>
          val currents = list.map(_.current)
          val avgCurrent = currents.sum/currents.length
          saveAlert(machine, avgCurrent)
      })
    }
  }

  def checkCurrent(machine: Machine) = {
    if (!machinesInAlert.contains(machine.name) && machine.current > machine.current_alert) { // send alert!
      machinesInAlert = machinesInAlert + machine.name
      createAlert(machine)
    } else if (machinesInAlert.contains(machine.name) && machine.current <= machine.current_alert) {
      machinesInAlert = machinesInAlert - machine.name  // remove machine from the list of machines in alert state
    }
  }

  def receive = {
    case MONITOR => {
      if (machines.isEmpty) {
        val futureResponse: Future[List[String]] = getMachines()

        futureResponse onComplete {
          case Success(data) => {
            machines = data
            for (machine <- machines) {
//              Logger.debug(machine)
              val mFutureResponse = getMachineInfo(machine)

              mFutureResponse onComplete {
                case Success(mac) => {
                  Logger.debug(mac.name)
                  checkCurrent(mac)
                  saveMachine(mac)
                }
                case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
              }
            }
          }
          case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
        }
      } else {
        for (machine <- machines) {
//            Logger.debug(machine)
          Thread.sleep(20)   // waiting sometime to not overload the API
          val mFutureResponse = getMachineInfo(machine)

          mFutureResponse onComplete {
            case Success(mac) => {
              Logger.debug(mac.name)
              saveMachine(mac)
            }
            case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
          }
        }
      }
    }
  }

}
