package akka.cache

import akka.actor.Actor

sealed trait Msg

object Msg {
  // Use Int for now because akka.util.Duration is not serializable
  // java.io.NotSerializableException: akka.util.Duration$$anon$1
  // 0 = Duration.Inf
  case class ContainsKey(key: Any) extends Msg
  case class Put(key: Any, value: Any, ttlSecs: Int = 0) extends Msg
  case class PutIfAbsent(key: Any, value: Any, ttlSecs: Int = 0) extends Msg
  case class PutIfAbsent2(key: Any, ttlSecs: Int = 0)(f: => Any) extends Msg
  case class Get(key: Any) extends Msg
  case class Remove(key: Any) extends Msg
  case object RemoveAll extends Msg
  case object GetStats extends Msg
}
