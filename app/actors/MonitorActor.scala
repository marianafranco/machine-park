package actors

import akka.actor.{Actor, Cancellable, Props}
import akka.contrib.throttle.Throttler.{Rate, SetTarget}
import akka.contrib.throttle.TimerBasedThrottler
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}
/**
 * Created by marianafranco on 25/11/15.
 */
class MonitorActor extends Actor with MonitorUtils {

  private val MONITOR = "MONITOR"

  private var scheduler: Cancellable = _

  private var machinesUrl  = List[String]()

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], new Rate(5, 100 millisecond)))

  override def preStart(): Unit = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 5 seconds,
      receiver = self,
      message = MONITOR
    )
    throttler ! SetTarget(Some(context.actorOf(Props[RequestThrottler])))
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case MONITOR =>
      val futureResponse: Future[List[String]] = getMachines

      futureResponse onComplete {
        case Success(data) => {
          machinesUrl = data
          context.actorOf(Props(new CorrelationActor(machinesUrl)), name = "CorrelationActor")
          context.become(initializedReceive)
        }
        case Failure(t) =>
          Logger.error("An error has occurred while getting all machines URLs: " + t.getMessage)
      }
  }

  val initializedReceive: Receive = {
    case MONITOR =>
      for (url <- machinesUrl) {
        throttler ! url
      }
  }
}


class RequestThrottler extends Actor with MonitorUtils {
  def receive = {
    case url: String =>
      retrieveMachineCurrent(url)
  }
}