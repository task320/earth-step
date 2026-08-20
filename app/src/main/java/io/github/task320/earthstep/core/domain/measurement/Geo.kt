package io.github.task320.earthstep.core.domain.measurement

import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * 球面上の距離・方位の計算(P2-2)。
 *
 * 徒歩の距離帯(数十m〜数km)では楕円体を使う Vincenty 法との差は無視できるため、
 * 実装が単純で数値的に安定な Haversine を採用する。
 */
object Geo {

    /** 地球の平均半径(m)。IUGG の平均半径。 */
    const val EARTH_RADIUS_METERS = 6_371_008.8

    private const val HALF = 2.0
    private const val FULL_CIRCLE_DEGREES = 360.0
    private const val STRAIGHT_ANGLE_DEGREES = 180.0

    /** 2点間の大円距離(m)。 */
    fun haversineMeters(latitude1: Double, longitude1: Double, latitude2: Double, longitude2: Double): Double {
        val lat1 = Math.toRadians(latitude1)
        val lat2 = Math.toRadians(latitude2)
        val deltaLat = lat2 - lat1
        val deltaLon = Math.toRadians(longitude2 - longitude1)

        val sinHalfLat = sin(deltaLat / HALF)
        val sinHalfLon = sin(deltaLon / HALF)
        val a = sinHalfLat * sinHalfLat + cos(lat1) * cos(lat2) * sinHalfLon * sinHalfLon
        // 数値誤差で a がわずかに1を超えると asin が NaN になるため丸める。
        return HALF * EARTH_RADIUS_METERS * asin(sqrt(min(1.0, a)))
    }

    fun distanceMeters(from: LocationSample, to: LocationSample): Double = haversineMeters(
        latitude1 = from.latitude,
        longitude1 = from.longitude,
        latitude2 = to.latitude,
        longitude2 = to.longitude,
    )

    /** [from] から [to] への方位角(度、真北0で時計回り)。 */
    fun bearingDegrees(from: LocationSample, to: LocationSample): Double {
        val lat1 = Math.toRadians(from.latitude)
        val lat2 = Math.toRadians(to.latitude)
        val deltaLon = Math.toRadians(to.longitude - from.longitude)

        val y = sin(deltaLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(deltaLon)
        return (Math.toDegrees(atan2(y, x)) + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES
    }

    /** 2つの方位角の差(0〜180度)。方向転換の大きさを測るのに使う(P2-20)。 */
    fun bearingDifferenceDegrees(from: Double, to: Double): Double {
        val diff = ((to - from + FULL_CIRCLE_DEGREES) % FULL_CIRCLE_DEGREES)
        return if (diff > STRAIGHT_ANGLE_DEGREES) FULL_CIRCLE_DEGREES - diff else diff
    }
}
