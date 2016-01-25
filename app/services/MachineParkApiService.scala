package services

import models.JsonFormats._
import models.{Environment, Machine}
import play.Logger
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequestHolder}

import scala.concurrent.Future

/**
 * Service responsible by perform all the requests to the external API.
 *
 * Created by marianafranco on 28/11/15.
 */
trait MachineParkApiService {

  private val MACHINE_PARK_API = "http://machinepark.actyx.io/api/v1"
  private val MACHINES_URL = MACHINE_PARK_API + "/machines"
  private val MACHINE_URL = MACHINE_PARK_API + "/machine/"
  private val ENV_URL = MACHINE_PARK_API + "/env-sensor"

  /**
   * Gets the machine's status.
   * @param url the machine's URL
   * @return the machine's current status
   */
  def getMachineInfo(url: String): Future[Machine] = {
    val id = url.substring(url.lastIndexOf("/") + 1)
    Logger.debug("Getting machine with id " + id)

    val holder: WSRequestHolder = WS.url(MACHINE_URL + id)
      .withHeaders("Accept" -> "application/json")

    val futureResponse: Future[Machine] = holder.get().map {
      response =>
        if (response.status == 200) {
          // removing the last part in the timestamp to be possible to parse it to joda datime
          val timestamp: String = response.json.\("timestamp").toString().dropRight(8).substring(1)
          (response.json.as[JsObject] - "timestamp" + ("timestamp", JsString(timestamp))).as[Machine]
        } else {
          throw new Exception("Request for machine " +  id + " fail with response status " + response.status)
        }
    }

    futureResponse
  }

  /**
   * Get machines URLs.
   * @return list of machine's URLs
   */
  def getMachines: Future[List[String]] = {
    Logger.debug("Getting all machines...")

    val holder: WSRequestHolder = WS.url(MACHINES_URL)
      .withHeaders("Accept" -> "application/json")

    val futureResponse: Future[List[String]] = holder.get().map {
      response =>
        response.json.as[List[String]]
    }

    futureResponse
  }

  /**
   * Gets the environmental data returned by the sensor (temperature, pressure, humidity).
   * @return the env status
   */
  def getEnvSensorData = {
    val holder: WSRequestHolder = WS.url(ENV_URL)
      .withHeaders("Accept" -> "application/json")

    val futureResponse: Future[Environment] = holder.get().map {
      response =>
        val pressure = response.json.\("pressure")(1)
        val temperature = response.json.\("temperature")(1)
        val humidity = response.json.\("humidity")(1)

        val jsonWithoutVariables = response.json.as[JsObject] - "pressure" - "temperature" - "humidity"

        (jsonWithoutVariables
          + ("pressure", pressure)
          + ("temperature", temperature)
          + ("humidity", humidity)).as[Environment]
    }

    futureResponse
  }
}
