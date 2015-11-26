package actors

import akka.actor.{Actor, Cancellable}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsArray
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

  private val CHECK_MACHINES = "CHECK_MACHINES"

  private val MACHINE_PARK_API = "http://machinepark.actyx.io/api/v1"
  private val MACHINES_URL = MACHINE_PARK_API + "/machines"

  private var scheduler: Cancellable = _

  override def preStart(): Unit = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 5 seconds,
      receiver = self,
      message = CHECK_MACHINES
    )
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case CHECK_MACHINES => {
      Logger.debug("Monitoring all machines...")

      val holder: WSRequestHolder = WS.url(MACHINES_URL)
      val complexHolder: WSRequestHolder =
        holder.withHeaders("Accept" -> "application/json")

      val futureResponse: Future[List[String]] = complexHolder.get().map {
        response =>
          (response.json).as[List[String]]
      }

      futureResponse onComplete {
        case Success(machines) => for (machine <- machines) Logger.debug(machine)
        case Failure(t) => println("An error has occured: " + t.getMessage)
      }
    }
  }
}
