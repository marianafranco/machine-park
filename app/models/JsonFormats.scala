package models

import play.api.libs.json.{Json, Reads}

/**
 * JSON formats used to implicit conversion of case classes to JSON and vice versa.
 *
 * Created by marianafranco on 19/01/16.
 */
object JsonFormats {
  // Generates Writes and Reads
  implicit val dateReads = Reads.jodaDateReads("yyyy-MM-dd'T'HH:mm:ss")
  implicit val machineFormat = Json.format[Machine]
  implicit val alertFormat = Json.format[Alert]
  implicit val envFormat = Json.format[Environment]
  implicit val machineEnvFormat = Json.format[MachineEnv]
}