package models

import java.util.Date

import play.modules.reactivemongo.ReactiveMongoPlugin
import play.modules.reactivemongo.json.collection.JSONCollection

/**
 * Created by marianafranco on 26/11/15.
 */
case class Machine( name: String,
                    timestamp: Date,
                    current: Double,
                    state: String,
                    location: String,
                    current_alert: Double,
                    `type`: String)

object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val machineFormat = Json.format[Machine]
}

object MongoModel {
  import play.api.Play.current

  // we can ignore "ReactiveMongoPlugin deprecated" warnings for play 2.3.x
  def db = ReactiveMongoPlugin.db

  def machinesCollection: JSONCollection = db.collection[JSONCollection]("machines")
}