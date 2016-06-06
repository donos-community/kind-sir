package kindSir.main

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import kindSir.actors._

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try


object ApplicationMain extends App {
  if (Try(ConfigFactory.load().getConfig("kindSir")).isFailure) {
    println("No config or wrong config specified")
  }
  else {
    val system = ActorSystem("KindSir")
    val groupsMonitor = system.actorOf(ConfigSupervisor.props, "config")
    Await.result(system.whenTerminated, Duration.Inf)
  }
}