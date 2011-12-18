package akka.cache

import akka.actor.Actor

// Use Int for now because akka.util.Duration is not serializable
// java.io.NotSerializableException: akka.util.Duration$$anon$1
// 0 = Duration.Inf
case class ContainsKey(key: Any)
case class Put(key: Any, value: Any, ttlSecs: Int = 0)
case class PutIfAbsent(key: Any, value: Any, ttlSecs: Int = 0)
case class PutIfAbsent2(key: Any, ttlSecs: Int = 0)(f: => Any)
case class Get[T](key: Any)
case class Remove(key: Any)
case class RemoveAll()
case class Stats()

class CacheActor(val limit: Int) extends Actor {
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

    case Stats =>
      self.reply(cache.stats)
  }
}
