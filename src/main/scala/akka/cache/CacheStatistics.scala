package akka.cache

class CacheStatistics(
  cachePuts:           Long,
  cacheGets:           Long,
  cacheHits:           Long,
  cacheHitPercentage:  Double,
  cacheMisses:         Long,
  cacheMissPercentage: Double,
  cacheRemovals:       Long,

  averagePutMillis:    Double,
  averageGetMillis:    Double,
  averageRemoveMillis: Double
) {
  override def toString = (
    "Puts:           " + cachePuts     + "\n" +
    "Gets:           " + cacheGets     + "\n" +
    "Hits:           " + cacheHits     + "\n" +
    "HitPercentage:  %.2f                 \n" +
    "Misses:         " + cacheMisses   + "\n" +
    "MissPercentage: %.2f                 \n" +
    "Removals:       " + cacheRemovals + "\n" +
    "AveragePut:     %.2f [ms]\n" +
    "AverageGet:     %.2f [ms]\n" +
    "AverageRemove:  %.2f [ms]\n"
  ).format(
    cacheHitPercentage * 100, cacheMissPercentage * 100,
    averagePutMillis, averageGetMillis, averageRemoveMillis
  )
}
