Machine Park
============

Application build to solve the Actyx Machine Park challenges: http://challenges.actyx.io/

### Technology Stack

- Backend:
  - Play Framework 2.3.x
  - MongoDB 3.0
  - [ReactiveMongo 0.11.7](http://reactivemongo.org/)
  - [akka-contrib 2.3.4](http://mvnrepository.com/artifact/com.typesafe.akka/akka-contrib_2.10/2.3.4) (for the [Throttling Actor Messages](http://doc.akka.io/docs/akka/snapshot/contrib/throttle.html))
  
- Frontend:
  - Bootstrap 3.3.6
  - AngularJS 1.4.8
  - [UI Bootstrap 1.0.3](https://angular-ui.github.io/bootstrap/)
  - [bootstrap-sidebar 0.2.2](https://github.com/asyraf9/bootstrap-sidebar)

### Building and Running

1. Update the `conf/application.conf` with the address of your MongoDB instance.
2. Open a command prompt in the folder of the application
3. Type `chmod u+x` for `activator` to execute it.
4. Type `./activator run`
5. Open "localhost:9000" in a browser


## Challenge 1: Power Usage Alert

An actor ([MonitorActor](https://github.com/marianafranco/machine-park/blob/master/app/actors/MonitorActor.scala)) makes a request for all machines each 5 seconds to capture their current. These requests are made via an [TimeBasedThrottler](http://doc.akka.io/docs/akka/snapshot/contrib/throttle.html) to not overload the external API. The throttler is configured to performe only 5 requests per milliseconds).

All machines' information are stored in a MongoDB [capped collection](https://docs.mongodb.org/manual/core/capped-collections/) which size was configured to store 20000 documents, more than it's needed to store 5 minutes of data.

Before saving a machine data in the db, the MonitorActor checks if the actual current is above the current threshold for that machine. In positive case, the actor calculates the average current drew by the machine in the last five minutes, and saves an alert with the average current in another capped collection.

The updates in the machines and alerts capped collections are streamed to the UI via websockets. The [WebSocketActor](https://github.com/marianafranco/machine-park/blob/master/app/actors/WebSocketActor.scala) gets a [tailable cursor](https://docs.mongodb.org/manual/tutorial/create-tailable-cursor/) over a capped collection, to asynchronously fetch newly inserted documents and push them into the websocket.


## Challenge 2: Environmental Correlation Analysis

TODO
