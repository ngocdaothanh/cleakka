package akka.cache

import akka.actor.ActorRef
import akka.dispatch.Future

trait WithActorRef {
  protected val ref: ActorRef
}

trait CacheActorRefApi {
  this: WithActorRef =>

  import Msg._

  def containsKey(key: Any) =
    (ref ? ContainsKey(key)).asInstanceOf[Future[Boolean]]

  def put(key: Any, value: Any, ttlSecs: Int = 0) {
    ref ! Put(key, value, ttlSecs)
  }

  def putIfAbsent(key: Any, value: Any, ttlSecs: Int = 0) =
    (ref ? PutIfAbsent(key, value, ttlSecs)).asInstanceOf[Future[Boolean]]

  def get(key: Any) =
    ref ? Get(key)

  def remove(key: Any) =
    (ref ? Remove(key)).asInstanceOf[Future[Boolean]]

  def removeAll {
    ref ! RemoveAll
  }

  def stats =
    (ref ? Stats).asInstanceOf[Future[Stats]]
}
