package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P2-2: 既知の距離と照合する。 */
class GeoTest {

    @Test
    fun `同じ点の距離は0になる`() {
        assertThat(Geo.haversineMeters(35.6586, 139.7454, 35.6586, 139.7454)).isEqualTo(0.0)
    }

    @Test
    fun `赤道上の経度1度は約111_3km`() {
        val meters = Geo.haversineMeters(0.0, 0.0, 0.0, 1.0)
        assertThat(meters).isWithin(200.0).of(111_195.0)
    }

    @Test
    fun `緯度1度はどの経度でも約111_2km`() {
        val atEquator = Geo.haversineMeters(0.0, 0.0, 1.0, 0.0)
        val atTokyo = Geo.haversineMeters(35.0, 139.0, 36.0, 139.0)
        assertThat(atEquator).isWithin(200.0).of(111_195.0)
        assertThat(atTokyo).isWithin(200.0).of(111_195.0)
    }

    @Test
    fun `東京タワーから東京スカイツリーまでは約8_2km`() {
        // 東京タワー(35.6586, 139.7454) と 東京スカイツリー(35.7101, 139.8107)。
        val meters = Geo.haversineMeters(35.6586, 139.7454, 35.7101, 139.8107)
        assertThat(meters).isWithin(150.0).of(8_200.0)
    }

    @Test
    fun `対蹠点までの距離は地球の半周になる`() {
        val meters = Geo.haversineMeters(0.0, 0.0, 0.0, 180.0)
        assertThat(meters).isWithin(1_000.0).of(Math.PI * Geo.EARTH_RADIUS_METERS)
    }

    @Test
    fun `方位角は真東で90度_真北で0度になる`() {
        assertThat(Geo.bearingDegrees(sample(0.0, 0.0), sample(0.0, 1.0))).isWithin(0.5).of(90.0)
        assertThat(Geo.bearingDegrees(sample(0.0, 0.0), sample(1.0, 0.0))).isWithin(0.5).of(0.0)
        assertThat(Geo.bearingDegrees(sample(0.0, 0.0), sample(0.0, -1.0))).isWithin(0.5).of(270.0)
    }

    @Test
    fun `方位差は0から180度の範囲で最短側を返す`() {
        assertThat(Geo.bearingDifferenceDegrees(10.0, 40.0)).isWithin(0.001).of(30.0)
        assertThat(Geo.bearingDifferenceDegrees(350.0, 10.0)).isWithin(0.001).of(20.0)
        assertThat(Geo.bearingDifferenceDegrees(10.0, 350.0)).isWithin(0.001).of(20.0)
        assertThat(Geo.bearingDifferenceDegrees(0.0, 180.0)).isWithin(0.001).of(180.0)
    }

    private fun sample(latitude: Double, longitude: Double) =
        io.github.task320.earthstep.core.domain.measurement.model.LocationSample(
            latitude = latitude,
            longitude = longitude,
            accuracyMeters = 5f,
            speedMetersPerSecond = null,
            timestampMillis = 0L,
        )
}
