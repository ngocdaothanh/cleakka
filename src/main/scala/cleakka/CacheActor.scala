package cleakka

import akka.actor.Actor
import akka.event.Logging

import Actor._

/** Thread-safe local cache. */
class CacheActor(val limit: Long) extends Actor {
  import Msg._

  private[this] val log   = Logging(context.system, this)
  private[this] val cache = new Cache(limit)

  override def postStop() {
    cache.removeAll()
  }

  def receive = {
    case ContainsKey(key) =>
      sender ! cache.containsKey(key)

    case Put(key, value, ttlSecs) =>
      cache.put(key, value, ttlSecs)

    case PutIfAbsent(key, value, ttlSecs) =>
      sender ! cache.putIfAbsent(key, value, ttlSecs)

    case Get(key) =>
      sender ! cache.get(key)

    case Remove(key) =>
      sender ! cache.remove(key)

    case RemoveAll =>
      cache.removeAll()

    case GetStats =>
      sender ! cache.getStats

    case other =>
      log.warning("Unknown message received: {}", other)
  }
}
