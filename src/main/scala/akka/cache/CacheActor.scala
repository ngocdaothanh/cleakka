package akka.cache

import akka.actor.{Actor, ActorRef}
import Actor._

/** Thread-safe local cache. */
class CacheActor(val limit: Int) extends Actor {
  import Msg._

  private var cache: Cache = _

  override def preStart() {
    cache = new Cache(limit)
  }

  override def postStop() {
    cache.removeAll()
  }

  def receive = {
    case ContainsKey(key) =>
      self.reply(cache.containsKey(key))

    case Put(key, value, ttlSecs) =>
      cache.put(key, value, ttlSecs)

    case PutIfAbsent(key, value, ttlSecs) =>
      self.reply(cache.putIfAbsent(key, value, ttlSecs))

    case Get(key) =>
      self.reply(cache.get(key))

    case Remove(key) =>
      self.reply(cache.remove(key))

    case RemoveAll =>
      cache.removeAll()

    case Stats =>
      self.reply(cache.stats)
  }

}
