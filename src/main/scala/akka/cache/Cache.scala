package akka.cache

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}
import java.nio.ByteBuffer
import scala.collection.mutable.{HashMap => MMap}

object Cache {
  val WATERMARK = 0.75
}

class Cache[K, V](val name: String, val limit: Int) {
  import Cache._

  private val data = new MMap[K, ByteBuffer]
  private var used = 0

  private var cachePuts:           Long = 0
  private var cacheGets:           Long = 0
  private var cacheHits:           Long = 0
  private var cacheMisses:         Long = 0
  private var cacheRemovals:       Long = 0

  private var totalGetMillis:      Long = 0
  private var totalePutMillis:     Long = 0
  private var totalRemoveMillis:   Long = 0

  //----------------------------------------------------------------------------

  def containsKey(key: K) = synchronized { data.isDefinedAt(key) }

  def put(key: K, value: V): Unit = synchronized {
    val t1 = System.currentTimeMillis

    val bytes     = serialize(value)
    val size      = bytes.length
    val remaining = limit - used
    var fit       = size <= remaining

    if (!fit) fit = evictUntilUnderWatermakAndFit(size)
    if (fit) {
      val buffer = ByteBuffer.allocateDirect(bytes.length)
      buffer.put(bytes)
      data(key)  = buffer
      used      += size
      cachePuts += 1
    }

    val t2 = System.currentTimeMillis
    totalePutMillis += t2 - t1
  }

  def putIfAbsent(key: K, value: V): Boolean = synchronized {
    if (data.isDefinedAt(key)) {
      false
    } else {
      put(key, value)
      true
    }
  }

  def putIfAbsent(key: K)(f: => V): Boolean = synchronized {
    if (data.isDefinedAt(key)) {
      false
    } else {
      val value: V = f
      put(key, value)
      true
    }
  }

  def get(key: K): Option[V] = synchronized {
    val t1  = System.currentTimeMillis

    val ret = data.get(key) match {
      case None =>
        cacheMisses += 1
        None

      case Some(buffer) =>
        cacheHits += 1
        buffer.rewind
        val bytes = new Array[Byte](buffer.capacity)
        buffer.get(bytes)
        deserialize(bytes)
    }

    cacheGets += 1
    val t2 = System.currentTimeMillis
    totalGetMillis += t2 - t1

    ret
  }

  def remove(key: K): Boolean = synchronized {
    val t1  = System.currentTimeMillis

    val ret = data.remove(key) match {
      case None         => false
      case Some(buffer) =>
        cacheRemovals += 1
        used -= buffer.capacity
        DirectByteBufferCleaner.clean(buffer)
        true
    }

    val t2 = System.currentTimeMillis
    totalRemoveMillis += t2 - t1

    ret
  }

  def removeAll(): Unit = synchronized {
    for (key <- data.keys) {
      val buffer = data.remove(key).get
      DirectByteBufferCleaner.clean(buffer)
    }
    cacheRemovals += 1
    used = 0
  }

  def getStatistics = synchronized {
    val cacheHitPercentage  = 1.0 * cacheHits   / cacheGets
    val cacheMissPercentage = 1.0 * cacheMisses / cacheGets

    val averagePutMillis    = totalePutMillis   / cachePuts
    val averageGetMillis    = totalGetMillis    / cacheGets
    val averageRemoveMillis = totalRemoveMillis / cacheRemovals

    new CacheStatistics(
      cachePuts,
      cacheGets,
      cacheHits,
      cacheHitPercentage,
      cacheMisses,
      cacheMissPercentage,
      cacheRemovals,
      averageGetMillis,
      averagePutMillis,
      averageRemoveMillis
    )
  }
  //----------------------------------------------------------------------------

  /** @return true if bytes of "size" can be put in cache */
  private def evictUntilUnderWatermakAndFit(size: Int): Boolean = {
    for (key <- data.keys) {
      val ratio     = 1.0 * used / limit
      val remaining = limit - used
      if (ratio > WATERMARK || remaining < size) {
        val buffer = data.remove(key).get
        DirectByteBufferCleaner.clean(buffer)
        used -= buffer.capacity
      } else {
        return true  // break the loop
      }
    }

    val remaining = limit - used
    remaining >= size
  }

  private def serialize(value: V): Array[Byte] = {
    val baos  = new ByteArrayOutputStream
    val oos   = new ObjectOutputStream(baos)
    oos.writeObject(value)
    val bytes = baos.toByteArray
    oos.close
    baos.close
    bytes
  }

  private def deserialize(bytes: Array[Byte]): Option[V] = {
    try {
      val bais  = new ByteArrayInputStream(bytes)
      val ois   = new ObjectInputStream(bais)
      val value = ois.readObject
      ois.close
      bais.close
      Some(value.asInstanceOf[V])
    } catch {
      case _ => None
    }
  }
}
