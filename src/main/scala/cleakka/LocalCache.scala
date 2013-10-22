package cleakka

/** This is just a thin wrapper for Cache that adds "synchronized". */
class LocalCache(override val limitInMB: Int) extends Cache(limitInMB) {
  override def isDefinedAt(key: Any) = synchronized {
    super.isDefinedAt(key)
  }

  override def put(key: Any, value: Any, ttlSecs: Int = 0) = synchronized {
    super.put(key, value, ttlSecs)
  }

  override def putIfAbsent(key: Any, value: Any) = synchronized {
    super.putIfAbsent(key, value)
  }

  override def putIfAbsent(key: Any, value: Any, ttlSecs: Int) = synchronized {
    super.putIfAbsent(key, value, ttlSecs)
  }

  override def putIfAbsent(key: Any, ttlSecs: Int = 0)(f: => Any) = synchronized {
    super.putIfAbsent(key, ttlSecs)(f)
  }

  override def get[T](key: Any) = synchronized {
    super.get(key)
  }

  override def remove(key: Any) = synchronized {
    super.remove(key)
  }

  override def removeAll() = synchronized {
    super.removeAll()
  }

  override def stats = synchronized {
    super.stats
  }
}
