package akka.cache

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import java.util.concurrent.TimeUnit.MILLISECONDS

import scala.collection.mutable.{HashMap => MMap}

import akka.util.Duration

object Cache {
  val WATERMARK = 0.75
}

/** This is not thread-safe. For thread-safe use CacheActor instead. */
class Cache(val limit: Int) {
  import Cache._

  //----------------------------------------------------------------------------

  private val data = new MMap[Any, Entry]
  private var used = 0

  private var cachePuts:       Long = 0
  private var cacheGets:       Long = 0
  private var cacheHits:       Long = 0
  private var cacheMisses:     Long = 0
  private var cacheRemovals:   Long = 0

  private var totalGetMillis:  Long = 0
  private var totalePutMillis: Long = 0

  //----------------------------------------------------------------------------

  def containsKey(key: Any) = data.isDefinedAt(key)

  def put(key: Any, value: Any, ttl: Duration = Duration.Inf) {
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

      data(key)  = new Entry(buffer, ttl, t1)
      used      += size
      cachePuts += 1
    }

    val t2           = System.currentTimeMillis
    totalePutMillis += t2 - t1
  }

  def putIfAbsent(key: Any, value: Any, ttl: Duration = Duration.Inf): Boolean = {
    data.get(key) match {
      case None =>
        put(key, value, ttl)
        true

      case Some(entry) =>
        entry.lastAccessed = System.currentTimeMillis
        false
    }
  }

  /**
   * Named "putIfAbsent2" to avoid error:
   * multiple overloaded alternatives of method putIfAbsent define default arguments
   *
   * http://stackoverflow.com/questions/4652095/why-does-the-scala-compiler-disallow-overloaded-methods-with-default-arguments
   */
  def putIfAbsent2(key: Any, ttl: Duration = Duration.Inf)(f: => Any): Boolean = {
    data.get(key) match {
      case None =>
        put(key, f, ttl)
        true

      case Some(entry) =>
        entry.lastAccessed = System.currentTimeMillis
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
        val dt     = t1 - entry.lastAccessed
        if (entry.ttl > Duration(dt, MILLISECONDS)) {
          cacheHits         += 1
          entry.lastAccessed = t1

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

  def stats = {
    val cacheHitPercentage  = 1.0 * cacheHits   / cacheGets
    val cacheMissPercentage = 1.0 * cacheMisses / cacheGets

    val averagePutMillis    = totalePutMillis   / cachePuts
    val averageGetMillis    = totalGetMillis    / cacheGets

    new CacheStatistics(
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
      if (ratio > WATERMARK || remaining < size) {
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
      case _ => None
    }
  }
}
