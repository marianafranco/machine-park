package actors

import akka.actor.{Actor, Cancellable}
import models.Machine
import models.JsonFormats._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws._
import play.api.Play.current
import play.Logger
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

/**
 * Created by marianafranco on 25/11/15.
 */
class MonitorActor extends Actor {

  private val MONITOR = "MONITOR"

  private val MACHINE_PARK_API = "http://machinepark.actyx.io/api/v1"
  private val MACHINES_URL = MACHINE_PARK_API + "/machines"
  private val MACHINE_URL = MACHINE_PARK_API + "/machine/"

  private var scheduler: Cancellable = _

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

  def getMachineInfo(url: String): Future[Machine] = {
    val id = url.substring(url.lastIndexOf("/") + 1)
    Logger.debug("Getting machine with id " + id)

    val holder: WSRequestHolder = WS.url(MACHINE_URL + id)
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")

    implicit val machineReads = Json.reads[Machine]

    val futureResponse: Future[Machine] = complexHolder.get().map {
      response =>
        (response.json).as[Machine]
    }

    return futureResponse
  }

  def getMachines(): Future[List[String]] = {
    Logger.debug("Getting all machines...")

    val holder: WSRequestHolder = WS.url(MACHINES_URL)
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")

    val futureResponse: Future[List[String]] = complexHolder.get().map {
      response =>
        (response.json).as[List[String]]
    }

    return futureResponse
  }

  def saveMachine(machine: Machine) = {
    import models.MongoModel._

    machinesCollection.insert(machine).onComplete {
      case Failure(e) => Logger.error("An error has occurred when saving a machine: " + e.getMessage)
      case Success(_) =>
    }
  }

  def receive = {
    case MONITOR => {
      val futureResponse: Future[List[String]] = getMachines()

      futureResponse onComplete {
        case Success(machines) => for (machine <- machines) {
//          Logger.debug(machine)
          val mFutureResponse = getMachineInfo(machine)

          mFutureResponse onComplete {
            case Success(machine) => {
              Logger.debug(machine.name)
              saveMachine(machine)
            }
            case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
          }

        }
        case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
      }
    }
  }

}
