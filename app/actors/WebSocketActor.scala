package actors

import akka.actor.{Actor, ActorRef}
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.{JsValue, Json}
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.QueryOpts

import scala.concurrent.Future

/**
 * Created by marianafranco on 24/01/16.
 */
class WebSocketActor(out: ActorRef, futureCollection: Future[JSONCollection], skipN: Int = 0) extends Actor {
  import context.dispatcher

  val cursor = futureCollection.map { collection =>
    collection
      .find(Json.obj())
      .options(QueryOpts().tailable.awaitData)
      .cursor[JsValue]()
      .enumerate()
      .apply(Iteratee.foreach(doc => out ! doc))
  }

  def receive = {
    case _ =>
  }
}
