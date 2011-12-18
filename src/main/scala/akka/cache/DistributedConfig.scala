package akka.cache

import java.net.InetSocketAddress
import akka.config.Configuration


object DistributedConfig {
  private val config = Configuration.fromFile("config/akka-cache.conf")

  val local = config.getBool("distributed.local", false)

  val remotes = config.getList("distributed.remotes").map(toHostPort _)

  private def toHostPort(host_colon_port: String): InetSocketAddress = {
    val host_port = host_colon_port.split(":")
    new InetSocketAddress(host_port(0), host_port(1).toInt)
  }
}
