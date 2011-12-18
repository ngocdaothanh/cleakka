package akka.cache

import java.nio.ByteBuffer
import akka.util.Duration

/** lastAccessed: [ms] */
class Entry(val directByteBuffer: ByteBuffer, val ttl: Duration, var lastAccessed: Long)
