package models

import play.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONInteger}

import scala.concurrent.Future

/**
 * Created by marianafranco on 19/01/16.
 */
object MongoCollections {

  //noinspection ScalaDeprecation
  // we can ignore "ReactiveMongoPlugin deprecated" warnings for play 2.3.x
  val db = ReactiveMongoPlugin.db

  def cappedCollection(name : String, max: Int, size: Int = 1024 * 1024) = {
    val collection = db.collection[JSONCollection](name)

    collection.stats().flatMap {
      case stats if !stats.capped =>
        // the collection is not capped, so we convert it
        Logger.debug("converting to capped")
        collection.convertToCapped(size, None)
      case _ => Future(collection)
    }.recover {
      // the collection does not exist, so we create it
      case _ =>
        Logger.debug("creating capped collection...")
        collection.createCapped(size, Some(max))
    }.map { _ =>
      Logger.debug("the capped collection is available")
      collection
    }
  }

  def ttlCollection(name: String) = {
    val collection = db.collection[JSONCollection](name)
    collection.indexesManager.ensure(
      Index(Seq(("timestamp", IndexType(BSONInteger(1)))), Some("expireAfterSeconds"),
        false, false, false, false, None, BSONDocument( "expireAfterSeconds" -> (60*60*24) )))
    collection
  }

  def machinesCollection: Future[JSONCollection] = cappedCollection("machines", 20000, 1024 * 1024 * 5)
  def alertsCollection: Future[JSONCollection] = cappedCollection("alerts", 500)

  def envMachinesCollection: JSONCollection = ttlCollection("env-machines")
}
