package cleakka

object Stats {
  def aggregate(xs: Seq[Stats]): Stats = {
    var cachePuts:           Long   = 0
    var cacheGets:           Long   = 0
    var cacheHits:           Long   = 0
    var cacheHitPercentage:  Double = 0
    var cacheMisses:         Long   = 0
    var cacheMissPercentage: Double = 0

    var averagePutMillis:    Double = 0
    var averageGetMillis:    Double = 0

    for (stats <-xs) {
      cachePuts           += stats.cachePuts
      cacheGets           += stats.cacheGets
      cacheHits           += stats.cacheHits
      cacheHitPercentage  += stats.cacheHitPercentage
      cacheMisses         += stats.cacheMisses
      cacheMissPercentage += stats.cacheMissPercentage
      averagePutMillis    += stats.averagePutMillis
      averageGetMillis    += stats.averageGetMillis
    }
    cacheHitPercentage  = cacheHitPercentage  / xs.length
    cacheMissPercentage = cacheMissPercentage / xs.length
    averagePutMillis    = averagePutMillis    / xs.length
    averageGetMillis    = averageGetMillis    / xs.length

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
}

class Stats(
  val cachePuts:           Long,
  val cacheGets:           Long,
  val cacheHits:           Long,
  val cacheHitPercentage:  Double,
  val cacheMisses:         Long,
  val cacheMissPercentage: Double,

  val averagePutMillis:    Double,
  val averageGetMillis:    Double
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
