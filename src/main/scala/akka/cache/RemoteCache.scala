package akka.cache

import akka.actor.{Actor, ActorRef}
import akka.event.EventHandler
import Actor._

object RemoteCacheActor {
  remote.start("localhost", 2552)
  remote.register("r", actorOf(new RemoteCacheActor))
}

class RemoteCacheActor extends Actor {
  private var local:   ActorRef        = _
  private var remotes: Array[ActorRef] = _

  override def preStart() {
    local = actorOf(new CacheActor(10240))
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
