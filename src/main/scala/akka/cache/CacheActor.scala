package akka.cache

import akka.actor.Actor

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

    case Put(key, value, ttl) =>
      cache.put(key, value, ttl)

    case PutIfAbsent(key, value, ttl) =>
      self.reply(cache.putIfAbsent(key, value))

    case Get(key) =>
      self.reply(cache.get(key))

    case Remove(key) =>
      self.reply(cache.remove(key))

    case RemoveAll =>
      cache.removeAll()

    case GetStats =>
      self.reply(cache.stats)
  }
}
