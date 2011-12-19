package akka.cache

import java.net.InetSocketAddress
import akka.config.Configuration

class DistributedConfig(val clientModeOnly: Boolean, val remotes: Seq[InetSocketAddress])

object DistributedConfig {
  lazy val default = fromFile("config/akka-cache.conf").get

  def fromFile(fileName: String): Option[DistributedConfig] = {
    try {
      val config = Configuration.fromFile(fileName)
      val clientModeOnly = config.getBool("distributed.clientModeOnly").get
      val remotes = config.getList("distributed.remotes").map(toHostPort _)
      Some (new DistributedConfig(clientModeOnly, remotes))
    } catch {
      case _ => None
    }
  }

  private def toHostPort(host_colon_port: String): InetSocketAddress = {
    val host_port = host_colon_port.split(":")
    new InetSocketAddress(host_port(0), host_port(1).toInt)
  }
}
