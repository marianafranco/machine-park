package actors

import akka.actor.{Actor, Cancellable}
import models.Machine
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
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
