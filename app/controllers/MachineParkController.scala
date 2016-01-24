package controllers

import models.MongoCollections._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json._
import reactivemongo.api.{Cursor, QueryOpts}

import scala.concurrent.Future

/**
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController {

  def socket = WebSocket.using[JsValue] { request =>

    val futureCount: Future[Int] = machinesCollection.map(_.count()).flatMap(x => x)

    val outEnumerator: Enumerator[JsValue] = {
      val futureEnumerator = machinesCollection.map { collection =>

        futureCount.map {
          case count =>
            val skip = count - 250

            // so we are sure that the collection exists and is a capped one
            val cursor: Cursor[JsValue] = collection
              // we want all the documents
              .find(Json.obj())
              // the cursor must be tailable and await data
              .options(QueryOpts(skipN = skip).tailable.awaitData)
              .cursor[JsValue]()

            // ok, let's enumerate it
            cursor.enumerate()
        }

      }
      Enumerator.flatten(futureEnumerator.flatMap(x => x))
    }

    val inIteratee = Iteratee.ignore[JsValue]

    (inIteratee, outEnumerator)
  }

  def alertSocket = WebSocket.using[JsValue] { request =>

    val outEnumerator = {
      val futureEnumerator = alertsCollection.map { collection =>
        // so we are sure that the collection exists and is a capped one
        val cursor: Cursor[JsValue] = collection
          // we want all the documents
          .find(Json.obj())
          // the cursor must be tailable and await data
          .options(QueryOpts().tailable.awaitData)
          .cursor[JsValue]()

        // ok, let's enumerate it
        cursor.enumerate()
      }
      Enumerator.flatten(futureEnumerator)
    }

    val inIteratee = Iteratee.ignore[JsValue]

    (inIteratee, outEnumerator)
  }
}
