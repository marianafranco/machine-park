package actors

import akka.actor.{Actor, Cancellable}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
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

  def getMachineInfo(url: String): Future[String] = {
    val id = url.substring(url.lastIndexOf("/") + 1)
    Logger.debug("Getting machine with id " + id)

    val holder: WSRequestHolder = WS.url(MACHINE_URL + id)
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")

    val futureResponse: Future[String] = complexHolder.get().map {
      response =>
        (response.json \ "name").as[String]
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


  def receive = {
    case MONITOR => {
      val futureResponse: Future[List[String]] = getMachines()

      futureResponse onComplete {
        case Success(machines) => for (machine <- machines) {
//          Logger.debug(machine)
          val mFutureResponse = getMachineInfo(machine)

          mFutureResponse onComplete {
            case Success(name) => Logger.debug(name)
            case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
          }

        }
        case Failure(t) => Logger.error("An error has occurred: " + t.getMessage)
      }
    }
  }

}
