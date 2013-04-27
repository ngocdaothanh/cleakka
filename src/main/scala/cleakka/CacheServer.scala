package cleakka

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object CacheServer {
  // TODO: Support scala.concurrent.duration.Duration

  sealed trait Msg

  case class  IsDefinedAt(key: Any)                                  extends Msg
  case class  Put        (key: Any, value: AnyRef, ttlSecs: Int = 0) extends Msg
  case class  PutIfAbsent(key: Any, value: AnyRef, ttlSecs: Int = 0) extends Msg
  case class  Get        (key: Any)                                  extends Msg
  case class  Remove     (key: Any)                                  extends Msg
  case object RemoveAll                                              extends Msg
  case object GetStats                                               extends Msg

  //----------------------------------------------------------------------------

  private[this] val SYSTEM_NAME = "cleanerakka"

  private[this] val system = ActorSystem(SYSTEM_NAME)

  def start(cacheName: String, limitInMB: Long): ActorRef = {
    system.actorOf(Props(new CacheServer(limitInMB)), remoteActorName(cacheName))
  }

  def lookUp(cacheName: String, host: String, port: Int): ActorRef = {
    val path = "akka://" + SYSTEM_NAME + "@" + host + ":" + port + "/user/" + remoteActorName(cacheName)
    system.actorFor(path)
  }

  /** Avoids naming conflict with other part of the system. */
  private def remoteActorName(cacheName: String) = getClass.getName + "-" + cacheName
}

/** An actor that wraps Cache. */
class CacheServer(limitInMB: Long) extends Actor {
  import CacheServer._

  private[this] val cache = new Cache(limitInMB)

  override def postStop() {
    cache.removeAll()
  }

  def receive = {
    case IsDefinedAt(key) =>
      sender ! cache.isDefinedAt(key)

    case Put(key, value, ttlSecs) =>
      cache.put(key, value, ttlSecs)

    case PutIfAbsent(key, value, ttlSecs) =>
      sender ! cache.putIfAbsent(key, value, ttlSecs)

    case Get(key) =>
      sender ! cache.get(key)

    case Remove(key) =>
      sender ! cache.remove(key)

    case RemoveAll =>
      cache.removeAll()

    case GetStats =>
      sender ! cache.stats

    case _ =>
  }
}
