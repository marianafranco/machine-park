package models

import org.joda.time.DateTime

/**
 * Type used to save the machine current with the environmental data.
 *
 * Created by marianafranco on 24/01/16.
 */
case class MachineEnv(name: String,
                      current: Double,
                      pressure: Double,
                      temperature: Double,
                      humidity: Double,
                      timestamp: DateTime = DateTime.now()) {

  def this(machine: Machine, env: Environment) {
    this(machine.name, machine.current, env.pressure, env.temperature, env.humidity)
  }
}
