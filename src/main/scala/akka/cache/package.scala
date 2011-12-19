package akka

import akka.actor.Actor

package object cache {
  /** Creates local cache actor. */
  def createLocal(limit: Long) = {
    val ref = Actor.actorOf(new CacheActor(limit))
    ref.start()
    new CacheActorRefApi(ref)
  }

  /** Registers cache actor on this node for other nodes to use. */
  def registerRemote(cacheName: String, limit: Long) = {
    val ref = Actor.actorOf(new CacheActor(limit))
    ref.start()
    Actor.remote.register(remoteActorName(cacheName), ref)
    new CacheActorRefApi(ref)
  }

  /** Gets cache actor from remote nodes. */
  def getRemote(cacheName: String, host: String, port: Int) = {
    val ref = Actor.remote.actorFor(remoteActorName(cacheName), host, port)
    new CacheActorRefApi(ref)
  }

  def getDistributed(cacheName: String, limit: Long) = {
    val ref = Actor.actorOf(new DistributedCacheActor(cacheName, limit))
    ref.start()
    new CacheActorRefApi(ref)
  }

  /** Avoids naming conflict with other part of the system. */
  private def remoteActorName(cacheName: String) = getClass.getName + "-" + cacheName
}
