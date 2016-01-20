package models

import org.joda.time.DateTime

/**
 * Created by marianafranco on 19/01/16.
 */
case class Alert(machineName: String,
                 timestamp: DateTime = DateTime.now(),
                 current: Double,
                 current_alert: Double,
                 current_avg: Double) {

  def this(machine: Machine, current_avg: Double) {
    this(machine.name, machine.timestamp, machine.current, machine.current_alert, current_avg)
  }
}
