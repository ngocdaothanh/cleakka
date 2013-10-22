package cleakka

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.routing.ConsistentHash
import akka.util.Timeout

class ConsistentHashCacheClient {
  import CacheServer._

  private var h = ConsistentHash[ActorRef](Seq(), 100)

  private[this] implicit val timeout = Timeout(5.seconds)

  def isDefinedAt(key: Any): Future[Boolean] =
    ask(h.nodeFor(key.toString), IsDefinedAt(key)).mapTo[Boolean]

  def put(key: Any, value: Any, ttlSecs: Int = 0) {
    h.nodeFor(key.toString) ! Put(key, value, ttlSecs)
  }

  def putIfAbsent(key: Any, value: Any, ttlSecs: Int = 0): Future[Boolean] =
    ask(h.nodeFor(key.toString), PutIfAbsent(key, value, ttlSecs)).mapTo[Boolean]

  def get[T: Manifest](key: Any): Future[T] =
    ask(h.nodeFor(key.toString), Get(key)).mapTo[T]

  def remove(key: Any): Future[Boolean] =
    ask(h.nodeFor(key.toString), Remove(key)).mapTo[Boolean]

  def removeAll() {
    //allRefs.foreach { _ ! RemoveAll }
  }

  /*
  def getStats: Future[Stats] = {
    // See Akka doc about Future http://akka.io/docs/
    val seqOfFutures = allRefs.map { ref => ask(ref, GetStats).mapTo[Stats] }
    val futureOfSeq  = Future.sequence(seqOfFutures)
    futureOfSeq.map { case seqOfStats =>
      Stats.aggregate(seqOfStats)
    }
  }
  */
}
