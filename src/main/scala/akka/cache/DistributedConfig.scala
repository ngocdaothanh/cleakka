package akka.cache

import java.io.File
import java.net.InetSocketAddress

import scala.collection.JavaConverters._
import scala.util.control.NonFatal

import com.typesafe.config.{Config, ConfigFactory}

class DistributedConfig(val clientModeOnly: Boolean, val remotes: Seq[InetSocketAddress])

object DistributedConfig {
  lazy val default = fromFile("config/akka-cache.conf").get

  def fromFile(fileName: String): Option[DistributedConfig] = {
    try {
      val config         = ConfigFactory.parseFile(new File(fileName))
      val clientModeOnly = config.getBoolean("distributed.clientModeOnly")
      val remotes        = config.getStringList("distributed.remotes").asScala.map(toHostPort _)
      Some(new DistributedConfig(clientModeOnly, remotes))
    } catch {
      case NonFatal(e) => None
    }
  }

  private def toHostPort(host_colon_port: String): InetSocketAddress = {
    val host_port = host_colon_port.split(":")
    new InetSocketAddress(host_port(0), host_port(1).toInt)
  }
}
