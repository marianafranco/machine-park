package models

import org.joda.time.DateTime

/**
 * Created by marianafranco on 26/11/15.
 */
case class Machine(name: String,
                   timestamp: DateTime = DateTime.now(),
                   current: Double,
                   state: String,
                   location: String,
                   current_alert: Double,
                   `type`: String)