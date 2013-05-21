package cleakka

import scala.concurrent.duration._

import akka.actor.ActorSystem
import akka.event.Logging
import akka.util.Timeout

object ActSys {
  val NAME    = "cleanerakka"
  val SYSTEM  = ActorSystem(NAME)
  val TIMEOUT = Timeout(5.seconds)
  val LOG     = Logging(SYSTEM, NAME)
}
