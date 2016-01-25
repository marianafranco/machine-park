package actors

import akka.actor.{Actor, Cancellable, Props}
import akka.contrib.throttle.Throttler.{Rate, SetTarget}
import akka.contrib.throttle.TimerBasedThrottler
import models.Environment
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.duration.DurationInt
import scala.language.postfixOps
import scala.util.{Failure, Success}

/**
 * Actor that retrieves the env-sensor information and the machines details each 1 minute.
 *
 * Created by marianafranco on 24/01/16.
 */
class EnvMonitorActor(machinesUrl: List[String]) extends Actor with MonitorUtils {

  private val MONITOR = "MONITOR"

  private var scheduler: Cancellable = _

  val throttler = context.actorOf(Props(classOf[TimerBasedThrottler], new Rate(10, 1 second)))

  override def preStart(): Unit = {
    scheduler = context.system.scheduler.schedule(
      initialDelay = 0 seconds,
      interval = 1 minutes,
      receiver = self,
      message = MONITOR
    )
    throttler ! SetTarget(Some(context.actorOf(Props[EnvRequestActor])))
  }

  override def postStop(): Unit = {
    scheduler.cancel()
  }

  def receive = {
    case MONITOR =>
      val eFutureResponse = getEnvSensorData

      eFutureResponse onComplete {
        case Success(env) =>
          for (url <- machinesUrl) {
            throttler ! (env, url)
          }

        case Failure(t) =>
          Logger.error("An error has occurred while getting the env sensor data: " + t.getMessage)
      }
  }

}

/**
 * Actor that performs the request to the external API to get a machine's status and saves it
 * together with the environmental data as a MachineEnv object in the db.
 */
class EnvRequestActor extends Actor with MonitorUtils {
  def receive = {
    case (env: Environment, url: String) =>
      val futureResponse = getMachineInfo(url)
      futureResponse onComplete {
        case Success(machine) =>
          saveMachineEnv(machine, env)

        case Failure(t) =>
          Logger.error("An error has occurred while getting the machines current: " + t.getMessage)
      }
  }
}
