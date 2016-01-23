package models

import play.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

import scala.concurrent.Future

/**
 * Created by marianafranco on 19/01/16.
 */
object MongoCollections {

  def cappedCollection(name : String, size: Int = 1024 * 1024) = {

    //noinspection ScalaDeprecation
    // we can ignore "ReactiveMongoPlugin deprecated" warnings for play 2.3.x
    val db = ReactiveMongoPlugin.db

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
        collection.createCapped(size, None)
    }.map { _ =>
      Logger.debug("the capped collection is available")
      collection
    }
  }

  def machinesCollection: Future[JSONCollection] = cappedCollection("machines")
  def alertsCollection: Future[JSONCollection] = cappedCollection("alerts")
}
