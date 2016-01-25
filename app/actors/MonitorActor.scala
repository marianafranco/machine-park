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
 * Makes a request for all machines each 5 seconds to capture their current.
 * These requests are made via an TimeBasedThrottler to not overload the external API.
 * The throttler is configured to perform only 5 requests per 100 milliseconds.
 *
 * Created by marianafranco on 25/11/15.
 */
class MonitorActor extends Actor with MonitorUtils {

  private val MONITOR = "MONITOR"

  private var scheduler: Cancellable = _

  // list of machines URLs to be invoked each 5 seconds
  private var machinesUrl  = List[String]()

  // throttler used to not overload the API
  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], new Rate(5, 100 millisecond)))

  // starts the scheduler that sends a message each 5 seconds to the actor monitor
  // the machines' current. Also sets the throttler target.
  override def preStart(): Unit = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 5 seconds,
      receiver = self,
      message = MONITOR
    )
    throttler ! SetTarget(Some(context.actorOf(Props[AlertActor])))
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case MONITOR =>
      // first, we try to get the machines URLs
      val futureResponse: Future[List[String]] = getMachines

      futureResponse onComplete {
        case Success(data) => {
          machinesUrl = data
          // starting the correlation actor
          context.actorOf(Props(new EnvMonitorActor(machinesUrl)), name = "CorrelationActor")
          context.become(initializedReceive)  // switching to the initializedReceive
        }
        case Failure(t) =>
          Logger.error("An error has occurred while getting all machines URLs: " + t.getMessage)
      }
  }

  // receive used after successfully retrieve the list of machines URLs
  val initializedReceive: Receive = {
    case MONITOR =>
      for (url <- machinesUrl) {
        throttler ! url
      }
  }
}

/**
 * Actor that retrieves the machines status and checks if the current is above the threshold.
 */
class AlertActor extends Actor with MonitorUtils {
  def receive = {
    case url: String =>
      getAndCheckMachineCurrent(url)
  }
}