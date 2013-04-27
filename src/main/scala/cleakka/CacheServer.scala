package cleakka

import java.net.URLEncoder
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.{
  Actor, ActorRef, ActorSystem, Props,
  ActorSelection, Identify, ActorIdentity
}
import akka.actor.ActorDSL._
import akka.pattern.ask
import akka.util.Timeout

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

  private[this] implicit val system  = ActorSystem(SYSTEM_NAME)
  private[this] implicit val timeout = Timeout(5.seconds)

  def start(cacheName: String, limitInMB: Long): ActorRef = {
    system.actorOf(Props(classOf[CacheServer], limitInMB), escapeActorName(cacheName))
  }

  def connect(cacheName: String): Future[Option[ActorRef]] = {
    val path = "/user/" + escapeActorName(cacheName)
    val sel  = system.actorSelection(path)
    actorSelection2ActorRef(sel)
  }

  def connect(cacheName: String, host: String, port: Int): Future[Option[ActorRef]] = {
    val path = "akka://" + SYSTEM_NAME + "@" + host + ":" + port + "/user/" + escapeActorName(cacheName)
    val sel  = system.actorSelection(path)
    actorSelection2ActorRef(sel)
  }

  private def escapeActorName(cacheName: String) = URLEncoder.encode(cacheName, "UTF-8")

  private def actorSelection2ActorRef(sel: ActorSelection): Future[Option[ActorRef]] = {
    val tmpRef = actor(new Act {
      var asker: ActorRef = _
      become {
        case "ask" =>
          asker = sender
          sel ! new Identify("dummy")

        case ActorIdentity(_, opt) =>
          asker ! opt
          context.stop(self)
      }
    })
    ask(tmpRef, "ask").mapTo[Option[ActorRef]]
  }
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
