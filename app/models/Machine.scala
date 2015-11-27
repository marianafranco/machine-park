package models

import java.util.Date

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