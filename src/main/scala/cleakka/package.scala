package cleakka

import akka.actor.{ActorSystem, Props}

package object cache {
  private val SYSTEM_NAME = "cleanerakka"

  val system = ActorSystem(SYSTEM_NAME)

  /** Creates local cache actor. */
  def createLocal(limit: Long) = {
    val ref = system.actorOf(Props(new CacheActor(limit)))
    new CacheActorRefApi(ref)
  }

  /** Registers cache actor on this node for other nodes to use. */
  def registerRemote(cacheName: String, limit: Long) = {
    val ref = system.actorOf(Props(new CacheActor(limit)), remoteActorName(cacheName))
    new CacheActorRefApi(ref)
  }

  /** Gets cache actor from remote nodes. */
  def getRemote(cacheName: String, host: String, port: Int) = {
    val path = "akka://" + SYSTEM_NAME + "@" + host + ":" + port + "/user/" + remoteActorName(cacheName)
    val ref = system.actorFor(path)
    new CacheActorRefApi(ref)
  }

  def getDistributed(cacheName: String, limit: Long) = {
    val ref = system.actorOf(Props(new DistributedCacheActor(cacheName, limit)))
    new CacheActorRefApi(ref)
  }

  /** Avoids naming conflict with other part of the system. */
  private def remoteActorName(cacheName: String) = getClass.getName + "-" + cacheName
}
