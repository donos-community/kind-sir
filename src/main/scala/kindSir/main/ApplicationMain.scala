package kindSir.main

import akka.actor.ActorSystem
import kindSir.actors._

import scala.concurrent.Await
import scala.concurrent.duration._


object ApplicationMain extends App {
  val system = ActorSystem("KindSir")
  val groupsMonitor = system.actorOf(ConfigSupervisor.props, "config")
  Await.result(system.whenTerminated, Duration.Inf)
}