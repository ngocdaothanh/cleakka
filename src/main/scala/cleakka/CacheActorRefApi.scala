package cleakka

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

class CacheActorRefApi(val ref: ActorRef) {
  import Msg._

  private[this] implicit val timeout = Timeout(5.seconds)

  def containsKey(key: Any) =
    ask(ref, ContainsKey(key)).mapTo[Boolean]

  def put(key: Any, value: Any, ttlSecs: Int = 0) {
    ref ! Put(key, value, ttlSecs)
  }

  def putIfAbsent(key: Any, value: Any, ttlSecs: Int = 0) =
    ask(ref, PutIfAbsent(key, value, ttlSecs)).mapTo[Boolean]

  def get(key: Any) =
    ask(ref, Get(key))

  def remove(key: Any) =
    ask(ref, Remove(key)).mapTo[Boolean]

  def removeAll() {
    ref ! RemoveAll
  }

  def getStats =
    ask(ref, GetStats).mapTo[Stats]
}
