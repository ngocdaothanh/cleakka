package akka.cache

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef}
import akka.dispatch.Future
import akka.event.EventHandler
import akka.remoteinterface._
import Actor._

/** Decides where to store the cache based on Ketama algorithm. */
class DistributedCacheActor(cacheName: String, limit: Long, config: DistributedConfig = DistributedConfig.default) extends Actor {
  import Msg._

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
    val addresses1 = config.remotes
    val addresses2 = if (config.clientModeOnly) addresses1 else addresses1 :+ remote.address
    val addresses3 = addresses2.sortWith(compareInetSocketAddress _)
    ketama = new Ketama(addresses3)

    allRefs = addresses3.map { case address =>
      if (config.clientModeOnly && remote.address != address)
        getRemote(cacheName, address.getHostName, address.getPort).ref
      else
        registerRemote(cacheName, limit).ref
    }

    val listener = actorOf(new Actor {
      def receive = {
        case RemoteServerClientConnected(server, clientAddress)    => println()
        case RemoteServerClientDisconnected(server, clientAddress) => //... act upon client disconnection
        case RemoteServerClientClosed(server, clientAddress)       => //... act upon client connection close
        case RemoteServerWriteFailed(request, cause, server, clientAddress) => //... act upon server write failure
      }
    }).start()
  }

  override def postStop() {
    // Stop local actor if used
    if (!config.clientModeOnly) {
      for ((address, i) <- ketama.addresses.zipWithIndex) {
        if (remote.address == address) {
          allRefs(i).stop()
          return
        }
      }
    }
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
      // http://akka.io/docs/akka/1.3-RC4/scala/futures.html
      val seqtOfFutures = allRefs.map { ref => (ref ? GetStats).asInstanceOf[Future[Stats]] }
      val futureOfSeq   = Future.sequence(seqtOfFutures)
      val seqOfStats    = futureOfSeq.get
      val stats         = Stats.aggregate(seqOfStats)
      self.reply(stats)

    case other =>
      EventHandler.warning(self, "Unknown message received: " + other)
  }
}
