package cleakka.test

import org.specs2.mutable._

class ServerSpec extends Specification {
  "Server" should {
    "be started" in {
      val serverRef = cleakka.CacheServer.start("x", 10)
      val client    = new cleakka.CacheClient(serverRef)
      client.put("hi", "2")

      true must equalTo(true)
    }

    "be stopped" in {
      val serverRef = cleakka.CacheServer.start("y", 10)
      val client    = new cleakka.CacheClient(serverRef)
      client.stopServer()

      true must equalTo(true)
    }
  }
}
