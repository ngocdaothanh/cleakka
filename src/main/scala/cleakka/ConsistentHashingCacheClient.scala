package cleakka

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout

class ConsistentHashingCacheClient {
  import CacheServer._

  private var ketama:  Ketama        = _
  private var allRefs: Seq[ActorRef] = _

  private[this] implicit val timeout = Timeout(5.seconds)

  def isDefinedAt(key: Any): Future[Boolean] =
    ask(allRefs(ketama.which(key)), IsDefinedAt(key)).mapTo[Boolean]

  def put(key: Any, value: AnyRef, ttlSecs: Int = 0) {
    allRefs(ketama.which(key)) ! Put(key, value, ttlSecs)
  }

  def putIfAbsent(key: Any, value: AnyRef, ttlSecs: Int = 0): Future[Boolean] =
    ask(allRefs(ketama.which(key)), PutIfAbsent(key, value, ttlSecs)).mapTo[Boolean]

  def get[T: Manifest](key: Any): Future[T] =
    ask(allRefs(ketama.which(key)), Get(key)).mapTo[T]

  def remove(key: Any): Future[Boolean] =
    ask(allRefs(ketama.which(key)), Remove(key)).mapTo[Boolean]

  def removeAll() {
    allRefs.foreach { _ ! RemoveAll }
  }

  def getStats: Future[Stats] = {
    // See Akka doc about Future http://akka.io/docs/
    val seqOfFutures = allRefs.map { ref => ask(ref, GetStats).mapTo[Stats] }
    val futureOfSeq  = Future.sequence(seqOfFutures)
    futureOfSeq.map { case seqOfStats =>
      Stats.aggregate(seqOfStats)
    }
  }
}
