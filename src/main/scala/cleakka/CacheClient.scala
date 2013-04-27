package cleakka

import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{ActorRef, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout

object CacheClient {
  def connect(cacheName: String, host: String, port: Int): CacheClient = {
    val serverRef = CacheServer.lookUp(cacheName, host, port)
    new CacheClient(serverRef)
  }
}

class CacheClient(val serverRef: ActorRef) {
  import CacheServer._

  private[this] implicit val timeout = Timeout(5.seconds)

  def isDefinedAt(key: Any): Future[Boolean] =
    ask(serverRef, IsDefinedAt(key)).mapTo[Boolean]

  def put(key: Any, value: AnyRef, ttlSecs: Int = 0) {
    serverRef ! Put(key, value, ttlSecs)
  }

  def putIfAbsent(key: Any, value: AnyRef, ttlSecs: Int = 0): Future[Boolean] =
    ask(serverRef, PutIfAbsent(key, value, ttlSecs)).mapTo[Boolean]

  def get[T: Manifest](key: Any): Future[T] =
    ask(serverRef, Get(key)).mapTo[T]

  def remove(key: Any): Future[Boolean] =
    ask(serverRef, Remove(key)).mapTo[Boolean]

  def removeAll() {
    serverRef ! RemoveAll
  }

  def getStats: Future[Stats] =
    ask(serverRef, GetStats).mapTo[Stats]

  def stopServer() {
    serverRef ! PoisonPill
  }
}
