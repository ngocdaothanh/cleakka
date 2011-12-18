package akka.cache

class Stats(
  cachePuts:           Long,
  cacheGets:           Long,
  cacheHits:           Long,
  cacheHitPercentage:  Double,
  cacheMisses:         Long,
  cacheMissPercentage: Double,

  averagePutMillis:    Double,
  averageGetMillis:    Double
) extends Serializable {
  override def toString = (
    "Puts:           " + cachePuts     + "\n" +
    "Gets:           " + cacheGets     + "\n" +
    "Hits:           " + cacheHits     + "\n" +
    "HitPercentage:  %.2f                 \n" +
    "Misses:         " + cacheMisses   + "\n" +
    "MissPercentage: %.2f                 \n" +
    "AveragePut:     %.2f [ms]\n" +
    "AverageGet:     %.2f [ms]\n"
  ).format(
    cacheHitPercentage  * 100,
    cacheMissPercentage * 100,
    averagePutMillis,
    averageGetMillis
  )
}
