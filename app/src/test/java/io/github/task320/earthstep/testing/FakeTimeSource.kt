package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.common.time.AppTimeSource
import java.time.Instant
import java.time.ZoneId

/**
 * 時刻とタイムゾーンを任意に動かせる [AppTimeSource]。日付境界のテスト(P1-8)で使う。
 *
 * @param elapsedMillis [current] に上乗せする経過時間。
 *   `delay` を含む Flow のテストでは `{ testScheduler.currentTime }` を渡し、
 *   仮想時間の進行と現在時刻を連動させる。
 */
class FakeTimeSource(
    var current: Instant = Instant.parse("2026-08-19T12:00:00Z"),
    var zoneId: ZoneId = ZoneId.of("Asia/Tokyo"),
    private val elapsedMillis: () -> Long = { 0L },
) : AppTimeSource {

    override fun now(): Instant = current.plusMillis(elapsedMillis())

    override fun zone(): ZoneId = zoneId
}
