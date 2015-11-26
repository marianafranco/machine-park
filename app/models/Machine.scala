package models

/**
 * Created by marianafranco on 26/11/15.
 */
case class Machine( name: String,
                    timestamp: String,
                    current: Float,
                    state: String,
                    location: String,
                    current_alert: Float,
                    `type`: String)

object JsonFormats {
  import play.api.libs.json.Json

  // Generates Writes and Reads for Feed and User thanks to Json Macros
  implicit val machineFormat = Json.format[Machine]
}