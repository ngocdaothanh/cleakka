package akka.cache

import java.net.InetSocketAddress

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

import akka.actor.{Actor, ActorRef, Props}
import akka.remote._
import akka.event.Logging
import akka.pattern.ask
import akka.util.Timeout

/** Decides where to store the cache based on Ketama algorithm. */
class DistributedCacheActor(
    cacheName: String,
    limit:     Long,
    config:    DistributedConfig = DistributedConfig.default
) extends Actor
{
  import Msg._

  private[this] implicit val timeout = Timeout(5.seconds)

  private[this] val log = Logging(context.system, this)

  private var ketama:  Ketama        = _
  private var allRefs: Seq[ActorRef] = _

  private def compareInetSocketAddress(a1: InetSocketAddress, a2: InetSocketAddress) = {
      if (a1.getHostName < a2.getHostName)
        true
      else if (a1.getHostName > a2.getHostName)
        false
      else
        a1.getPort <= a2.getPort
    }

  override def preStart() {
//    val addresses1 = config.remotes
//    val addresses2 = if (config.clientModeOnly) addresses1 else addresses1 :+ remote.address
//    val addresses3 = addresses2.sortWith(compareInetSocketAddress _)
//    ketama = new Ketama(addresses3)
//
//    allRefs = addresses3.map { case address =>
//      if (config.clientModeOnly && remote.address != address)
//        getRemote(cacheName, address.getHostName, address.getPort).ref
//      else
//        registerRemote(cacheName, limit).ref
//    }
//
//    val listener = system.actorOf(Props(new Actor {
//      def receive = {
//        case RemoteServerClientConnected(server, clientAddress)    => println()
//        case RemoteServerClientDisconnected(server, clientAddress) => //... act upon client disconnection
//        case RemoteServerClientClosed(server, clientAddress)       => //... act upon client connection close
//        case RemoteServerWriteFailed(request, cause, server, clientAddress) => //... act upon server write failure
//      }
//    }))
  }

  override def postStop() {
    // Stop local actor if used
//    if (!config.clientModeOnly) {
//      for ((address, i) <- ketama.addresses.zipWithIndex) {
//        if (remote.address == address) {
//          allRefs(i).stop()
//          return
//        }
//      }
//    }
  }

  def receive = {
    case m @ ContainsKey(key) =>
      allRefs(ketama.which(key)).forward(m)

    case m @ Put(key, value, ttlSecs) =>
      allRefs(ketama.which(key)).forward(m)

    case m @ PutIfAbsent(key, value, ttlSecs) =>
      allRefs(ketama.which(key)).forward(m)

    case m @ Get(key) =>
      allRefs(ketama.which(key)).forward(m)

    case m @ Remove(key) =>
      allRefs(ketama.which(key)).forward(m)

    case RemoveAll =>
      for (ref <- allRefs) ref ! RemoveAll

    case GetStats =>
      // See Akka doc about Future http://akka.io/docs/
      val seqOfFutures = allRefs.map { ref => ask(ref, GetStats).mapTo[Stats] }
      val futureOfSeq  = Future.sequence(seqOfFutures)
      futureOfSeq.onSuccess { case seqOfStats =>
        val stats = Stats.aggregate(seqOfStats)
        sender ! stats
      }

    case other =>
      log.warning("Unknown message received: {}", other)
  }
}
