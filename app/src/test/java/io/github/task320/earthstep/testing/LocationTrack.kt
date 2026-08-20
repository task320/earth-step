package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import kotlin.math.cos

/**
 * 擬似的な位置サンプル列を組み立てるためのビルダー(P2-11)。
 *
 * 緯度経度を直接書くとテストの意図が読めなくなるため、
 * 「北へ何m・東へ何m」という相対移動で書けるようにする。
 */
class LocationTrack(
    private val startLatitude: Double = 35.6586,
    private val startLongitude: Double = 139.7454,
    private val startTimestampMillis: Long = 1_755_650_400_000L,
    private val defaultAccuracyMeters: Float = 6f,
) {

    private val samples = mutableListOf<LocationSample>()
    private var latitude = startLatitude
    private var longitude = startLongitude
    private var timestampMillis = startTimestampMillis

    /** 現在地にサンプルを1件置く。 */
    fun mark(
        accuracyMeters: Float = defaultAccuracyMeters,
        speedMps: Float? = null,
        isMock: Boolean = false,
    ): LocationTrack = apply {
        samples += LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = accuracyMeters,
            speedMetersPerSecond = speedMps,
            timestampMillis = timestampMillis,
            isMock = isMock,
        )
    }

    /** 時間だけ進める。 */
    fun wait(millis: Long): LocationTrack = apply { timestampMillis += millis }

    /** 北へ [northMeters]、東へ [eastMeters] 進む。 */
    fun move(northMeters: Double = 0.0, eastMeters: Double = 0.0): LocationTrack = apply {
        latitude += northMeters / EARTH_RADIUS_METERS * DEGREES_PER_RADIAN
        longitude += eastMeters /
            (EARTH_RADIUS_METERS * cos(Math.toRadians(latitude))) * DEGREES_PER_RADIAN
    }

    /**
     * 東へ [eastMeters] 進んでからサンプルを置く、を [times] 回繰り返す。
     * @param intervalMillis 1回あたりの経過時間。
     */
    fun walkEast(
        times: Int,
        eastMeters: Double,
        intervalMillis: Long,
        accuracyMeters: Float = defaultAccuracyMeters,
        speedMps: Float? = null,
    ): LocationTrack = apply {
        repeat(times) {
            wait(intervalMillis)
            move(eastMeters = eastMeters)
            mark(accuracyMeters = accuracyMeters, speedMps = speedMps)
        }
    }

    fun build(): List<LocationSample> = samples.toList()

    private companion object {
        const val EARTH_RADIUS_METERS = 6_371_008.8
        const val DEGREES_PER_RADIAN = 180.0 / Math.PI
    }
}
