package controllers
import models.JsonFormats._
import models.MongoCollections._
import models.{Correlation, MachineEnv}
import play.api.libs.json.Json
import play.modules.reactivemongo.json._

import scala.concurrent.ExecutionContext.Implicits.global

/**
 * Methods used in the correlation calculation.
 *
 * Created by marianafranco on 25/01/16.
 */
trait CorrelationCalculation {

    /**
     * Gets all machine status with environmental data.
     * @param machineName the machine's name
     * @return list of MachineEnv
     */
    def getMachineEnvs(machineName: String) = {
        val selector = Json.obj(
            "name" -> machineName
        )
        envMachinesCollection.find(selector).cursor[MachineEnv]().collect[List]()
    }

    /**
     * Calculates the correlation between the machine's current and the env variables.
     * @param machineEnvList list of MachineEnv
     */
    def correlationsCalculation(machineEnvList: List[MachineEnv]): Correlation = {
        val tuple = machineEnvList.map(x => (x.current, x.temperature, x.pressure, x.humidity))

        val current = tuple.map(x => x._1)
        val temperature = tuple.map(x => x._2)
        val pressure = tuple.map(x => x._3)
        val humidity = tuple.map(x => x._4)

        val correlationTemperature = pearsonCorrelation(current, temperature)
        val correlationPressure = pearsonCorrelation(current, pressure)
        val correlationHumidity = pearsonCorrelation(current, humidity)

        Correlation(correlationTemperature, correlationPressure, correlationHumidity)
    }

    /**
     * Calculates the Pearson correlation between two variables.
     * @param var1
     * @param var2
     * @return the Pearson correlation
     */
    def pearsonCorrelation(var1: List[Double], var2: List[Double]) = {
        val meanVar1 = var1.sum / var1.length
        val meanVar2 = var2.sum / var2.length

        val diffVar1 = var1.map(x => x - meanVar1)
        val diffVar2 = var2.map(x => x - meanVar2)

        val stdDevVar1 = Math.sqrt(diffVar1.map(x => x * x).sum)
        val stdDevVar2 = Math.sqrt(diffVar2.map(x => x * x).sum)

        val pair = diffVar1 zip diffVar2

        val covariance = pair.map(x => x._1 * x._2).sum

        val correlation = covariance / (stdDevVar1 * stdDevVar2)
        BigDecimal(correlation).setScale(3, BigDecimal.RoundingMode.HALF_UP).toDouble
    }
}
