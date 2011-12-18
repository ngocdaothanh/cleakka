package akka.cache

import akka.actor.Actor._

/**
 * Convenient APIs. Started immediately after instantiated.
 *
 * The cacheName must have been registered by RemoteCacheActor.registered.
 */
class RemoteCacheActorRef(cacheName: String, host: String, port: Int) extends WithActorRef with CacheActorRefApi {
  protected val ref = remote.actorFor(RemoteCacheActor.actorName(cacheName), host, port)
  ref.start()
}
