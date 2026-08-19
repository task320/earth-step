package io.github.task320.earthstep.core.domain.model

import java.time.Instant

/**
 * 周回の記録(仕様6.1 `lap_record`)。
 *
 * @param lapNumber 周回番号。1周目は 1。
 * @param completedAt その周を走破した日時。進行中の周は null。
 */
data class LapRecord(val lapNumber: Int, val completedAt: Instant?)
