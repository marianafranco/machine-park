package services

import models.JsonFormats._
import models.Machine
import play.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.{WS, WSRequestHolder}
import play.api.Play.current

import scala.concurrent.Future

/**
 * Created by marianafranco on 28/11/15.
 */
trait MachineParkApiService {

  private val MACHINE_PARK_API = "http://machinepark.actyx.io/api/v1"
  private val MACHINES_URL = MACHINE_PARK_API + "/machines"
  private val MACHINE_URL = MACHINE_PARK_API + "/machine/"

  def getMachineInfo(url: String): Future[Machine] = {
    val id = url.substring(url.lastIndexOf("/") + 1)
    Logger.debug("Getting machine with id " + id)

    val holder: WSRequestHolder = WS.url(MACHINE_URL + id)
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")

    implicit val machineReads = Json.reads[Machine]

    val futureResponse: Future[Machine] = complexHolder.get().map {
      response =>
        (response.json).as[Machine]
    }

    return futureResponse
  }

  def getMachines(): Future[List[String]] = {
    Logger.debug("Getting all machines...")

    val holder: WSRequestHolder = WS.url(MACHINES_URL)
    val complexHolder: WSRequestHolder =
      holder.withHeaders("Accept" -> "application/json")

    val futureResponse: Future[List[String]] = complexHolder.get().map {
      response =>
        (response.json).as[List[String]]
    }

    return futureResponse
  }

}
