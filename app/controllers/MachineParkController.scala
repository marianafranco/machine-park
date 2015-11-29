package controllers

import models.MongoModel._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.mvc._
import play.api.libs.json._
import play.modules.reactivemongo.json._
import play.modules.reactivemongo.MongoController
import reactivemongo.api.{QueryOpts, Cursor}
import services.MachineParkApiService
import scala.concurrent.Future

/**
 * Created by marianafranco on 27/11/15.
 */
class MachineParkController extends Controller with MongoController with MachineParkApiService {

//  def getAllMachines = Action.async {
//    // let's do our query
//    val cursor: Cursor[Machine] = machinesCollection.
//      // find all
//      find(Json.obj()).
//      // sort them by creation date
//      sort(Json.obj("created" -> -1)).
//      // perform the query and get a cursor of JsObject
//      cursor[Machine]()
//
//    // gather all the JsObjects in a list
//    val futureMachinesList: Future[List[Machine]] = cursor.collect[List]()
//
//    // transform the list into a JsArray
//    val futureMachinesJsonArray: Future[JsArray] = futureMachinesList.map { machines =>
//      Json.arr(machines)
//    }
//    // everything's ok! Let's reply with the array
//    futureMachinesJsonArray.map {
//      machines =>
//        Ok(machines(0))
//    }
//  }

  def socket = WebSocket.using[JsValue] { request =>

    val outEnumerator = {
      val futureEnumerator = machinesCollection.map { collection =>
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

  def getMachinesUris = Action.async {
    val futureResponse: Future[List[String]] = getMachines()

    // transform the list into a JsArray
    val futureMachinesJsonArray: Future[JsArray] = futureResponse.map { machines =>
      Json.arr(machines)
    }

    // everything's ok! Let's reply with the array
    futureMachinesJsonArray.map {
      machines =>
        Ok(machines(0))
    }
  }
}
