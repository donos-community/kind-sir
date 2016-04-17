package kindSir.main

import akka.actor.ActorSystem
import kindSir.actors.GroupMonitor
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global


object ApplicationMain extends App {
  val system = ActorSystem("KindSir")
  val groupsMonitor = system.actorOf(GroupMonitor.props, "groupsMonitor")

  system.scheduler.scheduleOnce(60.seconds) {
    groupsMonitor ! GroupMonitor.ReloadConfig
  }

  Await.result(system.whenTerminated, Duration.Inf)
}