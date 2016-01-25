import actors.MonitorActor

import akka.actor.{ActorRef, Props}
import play.api.libs.concurrent.Akka
import play.api.{Logger, Application, GlobalSettings}
import play.api.Play.current

/**
 * Application global settings. Used to start the MonitorActor on start.
 *
 * Created by marianafranco on 25/11/15.
 */
object Global extends GlobalSettings {

  override def onStart(app: Application): Unit = {
    monitorDaemon(app)
  }

  def monitorDaemon(app: Application) = {
    Logger.info("Scheduling the monitor daemon")
    Akka.system.actorOf(Props[MonitorActor], name = "MonitorActor")
  }
}
