package models

import org.joda.time.DateTime
import play.Logger
import play.api.libs.json.Reads
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import scala.concurrent.{Future}

/**
 * Created by marianafranco on 26/11/15.
 */
case class Machine( name: String,
                    timestamp: DateTime = DateTime.now(),
                    requestTime: DateTime = DateTime.now(),
                    current: Double,
                    state: String,
                    location: String,
                    current_alert: Double,
                    `type`: String)

object JsonFormats {
  import play.api.libs.json.Json


  // Generates Writes and Reads
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss")
  implicit val machineFormat = Json.format[Machine]
}

object MongoModel {
  import play.api.Play.current
  import play.api.libs.concurrent.Execution.Implicits.defaultContext


  implicit def cappedCollection(name : String) = {
    // we can ignore "ReactiveMongoPlugin deprecated" warnings for play 2.3.x
    val db = ReactiveMongoPlugin.db

    val collection = db.collection[JSONCollection](name)

    collection.stats().flatMap {
      case stats if !stats.capped =>
        // the collection is not capped, so we convert it
        Logger.debug("converting to capped")
        collection.convertToCapped(1024 * 1024, None)
      case _ => Future(collection)
    }.recover {
      // the collection does not exist, so we create it
      case _ =>
        Logger.debug("creating capped collection...")
        collection.createCapped(1024 * 1024, None)
    }.map { _ =>
      Logger.debug("the capped collection is available")
      collection
    }
  }

  def machinesCollection: Future[JSONCollection] = cappedCollection("machines")
}