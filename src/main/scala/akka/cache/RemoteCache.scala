package akka.cache

import akka.actor.{Actor, ActorRef}
import Actor._

object RemoteCacheActor {
  remote.start("localhost", 2552)


  remote.register("r", actorOf(new RemoteCacheActor))
}

class RemoteCacheActor extends Actor {
  private var cacheActor: ActorRef = _

  override def preStart() {
    cacheActor = actorOf(new CacheActor(10240))
    cacheActor.start()
  }

  override def postStop() {
    cacheActor.stop()
  }

  def receive = {
    case m @ ContainsKey(key) =>
      cacheActor.forward(m)

    case m @ Put(key, value, ttl) =>
      cacheActor.forward(m)

    case m @ PutIfAbsent(key, value, ttl) =>
      cacheActor.forward(m)

    case m @ Get(key) =>
      cacheActor.forward(m)

    case m @ Remove(key) =>
      cacheActor.forward(m)

    case m @ RemoveAll =>
      cacheActor.forward(m)

    case m @ Stats =>
      cacheActor.forward(m)
  }
}
