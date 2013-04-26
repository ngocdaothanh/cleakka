package akka.cache

import java.nio.ByteBuffer
import scala.util.control.NonFatal

/**
 * Frees memory of direct buffer allocated by ByteBuffer.allocateDirect
 * without waiting for GC.
 *
 * See:
 * http://groups.google.com/group/netty/browse_thread/thread/3be7f573384af977
 * https://github.com/netty/netty/issues/62
 */
object DirectByteBufferCleaner {
  private val cleanerMethod = {
    try {
      val klass  = Class.forName("java.nio.DirectByteBuffer")
      val method = klass.getDeclaredMethod("cleaner")
      method.setAccessible(true)
      Some(method)
    } catch {
      case NonFatal(e) => None
    }
  }

  private val cleanMethod = {
    try {
      val klass  = Class.forName("sun.misc.Cleaner")
      val method = klass.getDeclaredMethod("clean")
      Some(method)
    } catch {
      case NonFatal(e) => None
    }
  }

  /** Does nothing if java.nio.DirectByteBuffer or sun.misc.Cleaner is not found */
  def clean(directByteBuffer: ByteBuffer) {
    if (cleanerMethod.isDefined && cleanMethod.isDefined) {
      val cleaner = cleanerMethod.get.invoke(directByteBuffer)
      cleanMethod.get.invoke(cleaner)
    }
  }
}
