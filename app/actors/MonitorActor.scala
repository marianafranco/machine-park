package actors

import akka.actor.{Actor, Cancellable}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.Logger
import scala.concurrent.duration.DurationInt

/**
 * Created by marianafranco on 25/11/15.
 */
class MonitorActor extends Actor {

  private val CHECK_MACHINES = "CHECK_MACHINES"

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
    }
  }
}
