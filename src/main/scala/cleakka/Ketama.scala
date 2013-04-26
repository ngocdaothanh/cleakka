package cleakka

import java.net.InetSocketAddress

class Ketama(val addresses: Seq[InetSocketAddress]) {
  /** @return index according to the given addresses */
  def which(key: Any): Int = {
    0
  }
}
