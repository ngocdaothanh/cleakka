package cleakka

sealed trait Msg

object Msg {
  // TODO: Support scala.concurrent.duration.Duration

  case class ContainsKey(key: Any) extends Msg
  case class Put(key: Any, value: AnyRef, ttlSecs: Int = 0) extends Msg
  case class PutIfAbsent(key: Any, value: AnyRef, ttlSecs: Int = 0) extends Msg
  case class Get(key: Any) extends Msg
  case class Remove(key: Any) extends Msg
  case object RemoveAll extends Msg
  case object GetStats extends Msg
}
