package actors

import akka.actor.{Actor, Cancellable}
import models.Machine
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
import play.api.libs.json.Json
import play.modules.reactivemongo.json._
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
        case Failure(e) => Logger.error("An error has occurred when saving a machine: " + e.getMessage)
        case Success(_) =>
      })
  }

  def receive = {
    case MONITOR => {
      if (machines.length == 0) {
        val futureResponse: Future[List[String]] = getMachines()

        futureResponse onComplete {
          case Success(data) => {
            machines = data
            for (machine <- machines) {
//              Logger.debug(machine)
              val mFutureResponse = getMachineInfo(machine)

              mFutureResponse onComplete {
                case Success(machine) => {
                  Logger.debug(machine.name)
                  saveMachine(machine)
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
            case Success(machine) => {
              Logger.debug(machine.name)
              saveMachine(machine)
            }
            case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
          }
        }
      }
    }
  }

}
