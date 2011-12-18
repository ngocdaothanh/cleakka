package akka.cache

import java.nio.ByteBuffer

/**
 * ttlSecs: 0 = Duration.Inf
 * lastAccessed: [ms]
 * lastAccessedSecs: Use Int instead of Long [ms] to save space
 */
class Entry(val directByteBuffer: ByteBuffer, val ttlSecs: Int, var lastAccessedSecs: Int)
