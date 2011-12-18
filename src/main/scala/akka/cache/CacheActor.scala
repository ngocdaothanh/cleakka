package akka.cache

import akka.actor.Actor
import akka.util.Duration

case class ContainsKey(key: Any)
case class Put(key: Any, value: Any, ttl: Duration = Duration.Inf)
case class PutIfAbsent(key: Any, value: Any, ttl: Duration = Duration.Inf)
case class PutIfAbsent2(key: Any, ttl: Duration = Duration.Inf)(f: => Any)
case class Get[T](key: Any)
case class Remove(key: Any)
case class RemoveAll()
case class Stats()

class CacheActor(val limit: Int) extends Actor {
  private val cache = new Cache(limit)

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
