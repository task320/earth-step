package io.github.task320.earthstep.core.domain.measurement

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.testing.LocationTrack
import org.junit.Test

/** P2-20: 直線が続けば間隔を空け、方向転換が多ければ詰める。 */
class LocationIntervalPlannerTest {

    private val config = MeasurementConfig()

    @Test
    fun `初期値は直線向けの間隔`() {
        val planner = LocationIntervalPlanner(config)

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalStraightMillis)
    }

    @Test
    fun `直線が続く間は間隔が変わらない`() {
        val planner = LocationIntervalPlanner(config)
        val samples = LocationTrack()
            .mark()
            .walkEast(times = 10, eastMeters = 20.0, intervalMillis = 10_000)
            .build()

        samples.forEach { planner.onLocation(it) }

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalStraightMillis)
    }

    @Test
    fun `方向転換が続くと間隔が短くなる`() {
        val planner = LocationIntervalPlanner(config)
        val track = LocationTrack().mark()
        // 20mごとに東・北・西・南と回る。毎区間 90 度曲がる。
        val directions = listOf(0.0 to 20.0, 20.0 to 0.0, 0.0 to -20.0, -20.0 to 0.0)
        repeat(3) {
            directions.forEach { (north, east) ->
                track.wait(5_000).move(northMeters = north, eastMeters = east).mark()
            }
        }

        track.build().forEach { planner.onLocation(it) }

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalTurningMillis)
    }

    @Test
    fun `曲がった後に直線へ戻ると間隔も戻る`() {
        val planner = LocationIntervalPlanner(config)
        val track = LocationTrack().mark()
        val directions = listOf(0.0 to 20.0, 20.0 to 0.0, 0.0 to -20.0, -20.0 to 0.0)
        repeat(3) {
            directions.forEach { (north, east) ->
                track.wait(5_000).move(northMeters = north, eastMeters = east).mark()
            }
        }
        track.build().forEach { planner.onLocation(it) }
        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalTurningMillis)

        val straight = LocationTrack(startTimestampMillis = 1_000_000L)
            .mark()
            .walkEast(times = 10, eastMeters = 20.0, intervalMillis = 10_000)
            .build()
        straight.forEach { planner.onLocation(it) }

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalStraightMillis)
    }

    @Test
    fun `ほとんど動いていない区間は方位の材料にしない`() {
        // 停止中のGPSの揺れで「曲がった」と誤認しないこと。
        val planner = LocationIntervalPlanner(config)
        val track = LocationTrack().mark()
        repeat(12) { index ->
            val north = if (index % 2 == 0) 1.0 else -1.0
            track.wait(5_000).move(northMeters = north, eastMeters = if (index % 3 == 0) 1.0 else -1.0).mark()
        }

        track.build().forEach { planner.onLocation(it) }

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalStraightMillis)
    }

    @Test
    fun `resetで初期状態へ戻る`() {
        val planner = LocationIntervalPlanner(config)
        val track = LocationTrack().mark()
        val directions = listOf(0.0 to 20.0, 20.0 to 0.0, 0.0 to -20.0, -20.0 to 0.0)
        repeat(3) {
            directions.forEach { (north, east) ->
                track.wait(5_000).move(northMeters = north, eastMeters = east).mark()
            }
        }
        track.build().forEach { planner.onLocation(it) }

        planner.reset()

        assertThat(planner.intervalMillis).isEqualTo(config.locationIntervalStraightMillis)
    }
}
