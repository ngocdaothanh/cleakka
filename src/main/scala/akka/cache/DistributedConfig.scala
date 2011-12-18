package akka.cache

import akka.config.Configuration

object DistributedConfig {
  private val config = Configuration.fromFile("config/akka-cache.conf")

  val local = toHostPort(config("distributed.local"))

  val remotes = config.getList("distributed.remotes").map(toHostPort(_))

  private def toHostPort(host_colon_port: String): (String, Int) = {
    val array = host_colon_port.split(":")
    (array(0), array(1).toInt)
  }
}
