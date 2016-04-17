package kindSir.actors

import akka.actor.{ActorLogging, Actor}

/**
  * Actor who reading Configuration for groups
  * and managing repo workers
  */
class GroupMonitor extends Actor with ActorLogging {

  def receive = {
    case _ => System.out.println("Received message, don't giving a shit.")
  }
}
