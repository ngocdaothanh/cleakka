package akka.cache

import akka.actor.Actor._

/** Convenient APIs. Started immediately after instantiated. */
class CacheActorRef(limit: Int) extends WithActorRef with CacheActorRefApi {
  protected val ref = actorOf(new CacheActor(limit))
  ref.start()
}
