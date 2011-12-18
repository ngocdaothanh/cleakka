package akka.cache

import akka.actor.{Actor, ActorRef}
import akka.event.EventHandler
import Actor._

object RemoteCacheActor {
  def register(cacheName: String, limit: Int) {
    remote.register(actorName(cacheName), actorOf(new RemoteCacheActor(limit)))
  }

  def actorName(cacheName: String) =
    classOf[RemoteCacheActor].getName + "-" + cacheName
}

/** Decides where to store the cache based on Ketama algorithm. */
class RemoteCacheActor(limit: Int) extends Actor {
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
