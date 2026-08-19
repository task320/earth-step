package io.github.task320.earthstep.core.common.time

import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/** 端末の時計とタイムゾーンをそのまま返す [AppTimeSource]。 */
@Singleton
class SystemTimeSource @Inject constructor() : AppTimeSource {

    override fun now(): Instant = Instant.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
