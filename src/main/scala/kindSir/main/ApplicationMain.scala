package kindSir.main

import akka.actor.ActorSystem
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ApplicationMain extends App {
  val system = ActorSystem("KindSir")
  val pingActor = system.actorOf(PingActor.props, "pingActor")
  pingActor ! PingActor.Initialize
  Await.result(system.whenTerminated, Duration.Inf)
}