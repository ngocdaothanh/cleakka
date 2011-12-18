package akka.cache

import akka.actor.{Actor, ActorRef}
import akka.event.EventHandler
import Actor._

/** Decides where to store the cache based on Ketama algorithm. */
class DistributedCacheActor(limit: Int) extends Actor {
  private var local:   ActorRef        = _
  private var remotes: Array[ActorRef] = _

  override def preStart() {
    local = actorOf(new CacheActor(limit))
    local.start()
  }

  override def postStop() {
    local.stop()
  }

  def receive = {
    case m: Msg => local.forward(m)
    case other => EventHandler.warning(self, "Unknown message received: " + other)
  }
}
