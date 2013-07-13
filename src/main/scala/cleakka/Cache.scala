package cleakka

import java.nio.ByteBuffer
import scala.util.control.Exception.allCatch

import com.esotericsoftware.kryo.io.{ByteBufferInputStream, Input}

import scala.collection.mutable.{HashMap => MMap}
import scala.util.control.NonFatal

object Cache {
  val WATERMARK = 0.75

  /**
   * ttlSecs: 0 = Duration.Inf
   * lastAccessed: [ms]
   * lastAccessedSecs: Use Int instead of Long [ms] to save space
   */
  class Entry(val directByteBuffer: ByteBuffer, val ttlSecs: Int, var lastAccessedSecs: Int)
}

/** Non thread-safe cache storage. For thread-safe, use LocalCache or ClusterCache. */
class Cache(val limitInMB: Int) {
  import Cache._

  private[this] val limit = limitInMB.toLong * 1024L * 1024L

  private[this] val data = new MMap[Any, Entry]
  private[this] var used = 0L  // Important, must be Long, not Int

  private[this] var cachePuts       = 0L
  private[this] var cacheGets       = 0L
  private[this] var cacheHits       = 0L
  private[this] var cacheMisses     = 0L
  private[this] var cacheRemovals   = 0L

  private[this] var totalGetMillis  = 0L
  private[this] var totalPutMillis  = 0L

  //----------------------------------------------------------------------------

  def isDefinedAt(key: Any) = data.isDefinedAt(key)

  /** @param ttlSecs Non positive means no TTL */
  def put(key: Any, value: AnyRef, ttlSecs: Int = 0) {
    val t1Ms = System.currentTimeMillis()
    val t1S  = (t1Ms / 1000).toInt

    data.get(key).foreach { lastEntry =>
      val buffer = lastEntry.directByteBuffer
      data.remove(key)
      used -= buffer.capacity
      DirectByteBufferCleaner.clean(buffer)
    }

    val bytes     = InputStreamKryoInjection(value)
    val size      = bytes.length
    val remaining = limit - used
    var fit       = size <= remaining

    if (!fit) fit = evictUntilUnderWatermakAndFit(size)
    if (fit) {
      val buffer = ByteBuffer.allocateDirect(bytes.length)
      buffer.put(bytes)

      data(key)  = new Entry(buffer, ttlSecs, t1S)
      used      += size
      cachePuts += 1
    }

    val t2Ms        = System.currentTimeMillis()
    totalPutMillis += t2Ms - t1Ms
  }

  def putIfAbsent(key: Any, value: AnyRef): Boolean = {
    putIfAbsent(key, value, 0)
  }

  /** @param ttlSecs Non positive means no TTL */
  def putIfAbsent(key: Any, value: AnyRef, ttlSecs: Int): Boolean = {
    data.get(key) match {
      case None =>
        put(key, value, ttlSecs)
        true

      case Some(entry) =>
        entry.lastAccessedSecs = (System.currentTimeMillis() / 1000).toInt
        false
    }
  }

  /** @param ttlSecs Non positive means no TTL */
  def putIfAbsent(key: Any, ttlSecs: Int = 0)(f: => AnyRef): Boolean = {
    data.get(key) match {
      case None =>
        put(key, f, ttlSecs)
        true

      case Some(entry) =>
        entry.lastAccessedSecs = (System.currentTimeMillis() / 1000).toInt
        false
    }
  }

  def get[T](key: Any): Option[T] = {
    val t1Ms = System.currentTimeMillis()
    val t1S  = (t1Ms / 1000).toInt

    cacheGets += 1
    val ret = data.get(key) match {
      case None =>
        cacheMisses += 1
        None

      case Some(entry) =>
        val buffer = entry.directByteBuffer
        val dtSecs = t1S - entry.lastAccessedSecs
        if (entry.ttlSecs <= 0 || entry.ttlSecs > dtSecs) {
          cacheHits             += 1
          entry.lastAccessedSecs = t1S

          buffer.rewind()
          val bbis = new ByteBufferInputStream(buffer)
          InputStreamKryoInjection.invert(bbis).asInstanceOf[Option[T]]
        } else {
          cacheMisses += 1
          data.remove(key)
          used -= buffer.capacity
          DirectByteBufferCleaner.clean(buffer)
          None
        }
    }

    val t2Ms = System.currentTimeMillis
    totalGetMillis += t2Ms - t1Ms
    ret
  }

  def remove(key: Any): Boolean = {
    data.remove(key) match {
      case None => false

      case Some(entry) =>
        val buffer     = entry.directByteBuffer
        cacheRemovals += 1
        used          -= buffer.capacity
        DirectByteBufferCleaner.clean(buffer)
        true
    }
  }

  def removeAll() {
    val it = data.iterator
    while (it.hasNext) {
      val entry = it.next()._2
      DirectByteBufferCleaner.clean(entry.directByteBuffer)
    }
    data.clear()
    cacheRemovals += 1
    used           = 0
  }

  def stats = {
    val cacheHitPercentage  = if (cacheGets > 0) 1.0 * cacheHits   / cacheGets else 0
    val cacheMissPercentage = if (cacheGets > 0) 1.0 * cacheMisses / cacheGets else 0

    val averagePutMillis    = if (cachePuts > 0) totalPutMillis    / cachePuts else 0
    val averageGetMillis    = if (cacheGets > 0) totalGetMillis    / cacheGets else 0

    new Stats(
      cachePuts,
      cacheGets,
      cacheHits,
      cacheHitPercentage,
      cacheMisses,
      cacheMissPercentage,
      averagePutMillis,
      averageGetMillis
    )
  }

  //----------------------------------------------------------------------------

  /** @return true if bytes of "size" can be put in cache */
  private def evictUntilUnderWatermakAndFit(size: Int): Boolean = {
    val it = data.iterator

    var ratio     = 1.0 * used / limit
    var remaining = limit - used
    var done      = !it.hasNext || (ratio <= Cache.WATERMARK && remaining >= size)
    while (!done) {
      val (key, entry) = it.next()
      data.remove(key)

      val buffer = entry.directByteBuffer
      DirectByteBufferCleaner.clean(buffer)

      used     -= buffer.capacity
      ratio     = 1.0 * used / limit
      remaining = limit - used
      done      = !it.hasNext || (ratio <= Cache.WATERMARK && remaining >= size)
    }

    remaining >= size
  }
}
