package io.github.task320.earthstep.core.domain.model

import java.time.LocalDate

/**
 * 1日ぶんの距離(仕様6.1 `daily_log`)。
 * 日付キーはローカルタイムゾーン基準(P1-8)。
 */
data class DailyDistance(val date: LocalDate, val distanceMeters: Long)
