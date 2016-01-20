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
                    current: Double,
                    state: String,
                    location: String,
                    current_alert: Double,
                    `type`: String)