package controllers

import models.MongoCollections._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json._
import play.api.mvc._
import play.modules.reactivemongo.MongoController
import play.modules.reactivemongo.json._
import reactivemongo.api.{Cursor, QueryOpts}

/**
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController {

  def socket = WebSocket.using[JsValue] { request =>

    val outEnumerator = {
      val futureEnumerator = machinesCollection.map { collection =>
        // so we are sure that the collection exists and is a capped one
        val cursor: Cursor[JsValue] = collection
          // we want all the documents
          .find(Json.obj())
          // the cursor must be tailable and await data
          .options(QueryOpts(skipN = 5000).tailable.awaitData)
          .cursor[JsValue]()

        // ok, let's enumerate it
        cursor.enumerate()
      }
      Enumerator.flatten(futureEnumerator)
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
