package akka.cache

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer

import scala.collection.mutable.{HashMap => MMap}
import scala.util.control.NonFatal

import akka.actor.Actor
import Actor._

object Cache {
  val WATERMARK = 0.75
}

/** Non thread-safe local cache. For thread-safe use CacheActor instead. */
class Cache(val limit: Long) {
  private val data = new MMap[Any, Entry]
  private var used = 0L  // Important, must be Long, not Int

  private var cachePuts       = 0L
  private var cacheGets       = 0L
  private var cacheHits       = 0L
  private var cacheMisses     = 0L
  private var cacheRemovals   = 0L

  private var totalGetMillis  = 0L
  private var totalePutMillis = 0L

  //----------------------------------------------------------------------------

  def containsKey(key: Any) = data.isDefinedAt(key)

  def put(key: Any, value: Any, ttlSecs: Int = 0) {
    val t1 = System.currentTimeMillis

    val lastEntryo = data.get(key)
    if (lastEntryo.isDefined) {
      val buffer = lastEntryo.get.directByteBuffer
      data.remove(key)
      used -= buffer.capacity
      DirectByteBufferCleaner.clean(buffer)
    }

    val bytes     = serialize(value)
    val size      = bytes.length
    val remaining = limit - used
    var fit       = size <= remaining

    if (!fit) fit = evictUntilUnderWatermakAndFit(size)
    if (fit) {
      val buffer = ByteBuffer.allocateDirect(bytes.length)
      buffer.put(bytes)

      data(key)  = new Entry(buffer, ttlSecs, (t1 / 1000).toInt)
      used      += size
      cachePuts += 1
    }

    val t2           = System.currentTimeMillis
    totalePutMillis += t2 - t1
  }

  def putIfAbsent(key: Any, value: Any, ttlSecs: Int = 0): Boolean = {
    data.get(key) match {
      case None =>
        put(key, value, ttlSecs)
        true

      case Some(entry) =>
        entry.lastAccessedSecs = (System.currentTimeMillis / 1000).toInt
        false
    }
  }

  /**
   * Named "putIfAbsent2" to avoid error:
   * multiple overloaded alternatives of method putIfAbsent define default arguments
   *
   * http://stackoverflow.com/questions/4652095/why-does-the-scala-compiler-disallow-overloaded-methods-with-default-arguments
   */
  def putIfAbsent2(key: Any, ttlSecs: Int = 0)(f: => Any): Boolean = {
    data.get(key) match {
      case None =>
        put(key, f, ttlSecs)
        true

      case Some(entry) =>
        entry.lastAccessedSecs = (System.currentTimeMillis / 1000).toInt
        false
    }
  }

  def get[T](key: Any): Option[T] = {
    val t1 = System.currentTimeMillis

    cacheGets += 1
    val ret = data.get(key) match {
      case None =>
        cacheMisses += 1
        None

      case Some(entry) =>
        val buffer = entry.directByteBuffer
        val dtSecs = t1 / 1000 - entry.lastAccessedSecs
        if (entry.ttlSecs <= 0 || entry.ttlSecs > dtSecs) {
          cacheHits             += 1
          entry.lastAccessedSecs = (t1 / 1000).toInt

          buffer.rewind
          val bytes = new Array[Byte](buffer.capacity)
          buffer.get(bytes)
          deserialize(bytes)
        } else {
          cacheMisses += 1
          data.remove(key)
          used -= buffer.capacity
          DirectByteBufferCleaner.clean(buffer)
          None
        }
    }

    val t2 = System.currentTimeMillis
    totalGetMillis += t2 - t1

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
    for (entry <- data.values) DirectByteBufferCleaner.clean(entry.directByteBuffer)
    data.clear()
    cacheRemovals += 1
    used           = 0
  }

  def getStats = {
    val cacheHitPercentage  = if (cacheGets > 0) 1.0 * cacheHits   / cacheGets else 0
    val cacheMissPercentage = if (cacheGets > 0) 1.0 * cacheMisses / cacheGets else 0

    val averagePutMillis    = if (cachePuts > 0) totalePutMillis   / cachePuts else 0
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
    for (key <- data.keys) {
      val ratio     = 1.0 * used / limit
      val remaining = limit - used
      if (ratio > Cache.WATERMARK || remaining < size) {
        val entry  = data.remove(key).get
        val buffer = entry.directByteBuffer
        used      -= buffer.capacity
        DirectByteBufferCleaner.clean(buffer)
      } else {
        return true  // break the loop
      }
    }

    val remaining = limit - used
    remaining >= size
  }

  private def serialize(value: Any): Array[Byte] = {
    val baos  = new ByteArrayOutputStream
    val oos   = new ObjectOutputStream(baos)
    oos.writeObject(value)
    val bytes = baos.toByteArray
    oos.close
    baos.close
    bytes
  }

  private def deserialize[T](bytes: Array[Byte]): Option[T] = {
    try {
      val bais  = new ByteArrayInputStream(bytes)
      val ois   = new ObjectInputStream(bais)
      val value = ois.readObject
      ois.close
      bais.close
      Some(value.asInstanceOf[T])
    } catch {
      case NonFatal(e) => None
    }
  }
}
