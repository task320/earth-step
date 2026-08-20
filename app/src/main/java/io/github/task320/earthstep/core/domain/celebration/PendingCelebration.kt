package io.github.task320.earthstep.core.domain.celebration

import io.github.task320.earthstep.core.domain.milestone.MilestoneCatalog
import io.github.task320.earthstep.core.domain.progress.LapMarker
import io.github.task320.earthstep.core.domain.progress.ProgressEvent

/**
 * まだ再生していない演出(P5-13)。
 *
 * 達成はバックグラウンドでも起きるため、演出をその場で出せるとは限らない。
 * 未再生ぶんを貯めておき、次にアプリを開いたときに順番に再生する。
 *
 * @param totalDistanceMeters その出来事が起きた時点の累計距離。再生順の基準になる。
 */
sealed interface PendingCelebration {

    val totalDistanceMeters: Long

    /** 大台演出(仕様3.4)で見せるか。 */
    val isMajor: Boolean

    data class Milestone(val milestoneIndex: Int, override val totalDistanceMeters: Long) : PendingCelebration {
        override val isMajor: Boolean
            get() = MilestoneCatalog.byIndex(milestoneIndex)?.isMajor == true
    }

    data class Marker(val marker: LapMarker, val lapNumber: Int, override val totalDistanceMeters: Long) :
        PendingCelebration {
        override val isMajor: Boolean get() = true
    }

    data class Lap(val lapNumber: Int, override val totalDistanceMeters: Long) : PendingCelebration {
        override val isMajor: Boolean get() = true
    }

    companion object {

        /** [ProgressEvent] を保存できる形へ移す。 */
        fun from(event: ProgressEvent): PendingCelebration = when (event) {
            is ProgressEvent.MilestoneAchieved ->
                Milestone(event.milestone.index, event.totalDistanceMeters)

            is ProgressEvent.LapMarkerReached ->
                Marker(event.marker, event.lapNumber, event.totalDistanceMeters)

            is ProgressEvent.LapCompleted ->
                Lap(event.lapNumber, event.totalDistanceMeters)
        }
    }
}

/**
 * 演出キューの文字列エンコード。
 *
 * 保存先は DataStore の文字列1本。専用のシリアライズ機構を持ち込むほどの構造ではないし、
 * 中身が壊れていたら「演出を1回取りこぼす」だけで済むため、読めない行は黙って捨てる。
 */
object PendingCelebrationCodec {

    private const val FIELD_SEPARATOR = "|"
    private const val RECORD_SEPARATOR = "\n"

    private const val TYPE_MILESTONE = "M"
    private const val TYPE_MARKER = "K"
    private const val TYPE_LAP = "L"

    fun encode(celebrations: List<PendingCelebration>): String =
        celebrations.joinToString(RECORD_SEPARATOR, transform = ::encodeOne)

    fun decode(encoded: String): List<PendingCelebration> = encoded
        .split(RECORD_SEPARATOR)
        .mapNotNull { decodeOne(it) }

    private fun encodeOne(celebration: PendingCelebration): String = when (celebration) {
        is PendingCelebration.Milestone -> listOf(
            TYPE_MILESTONE,
            celebration.milestoneIndex,
            celebration.totalDistanceMeters,
        )

        is PendingCelebration.Marker -> listOf(
            TYPE_MARKER,
            celebration.marker.name,
            celebration.lapNumber,
            celebration.totalDistanceMeters,
        )

        is PendingCelebration.Lap -> listOf(
            TYPE_LAP,
            celebration.lapNumber,
            celebration.totalDistanceMeters,
        )
    }.joinToString(FIELD_SEPARATOR)

    private fun decodeOne(line: String): PendingCelebration? {
        val fields = line.trim().split(FIELD_SEPARATOR)
        return runCatching {
            when (fields.firstOrNull()) {
                TYPE_MILESTONE -> PendingCelebration.Milestone(
                    milestoneIndex = fields[1].toInt(),
                    totalDistanceMeters = fields[2].toLong(),
                )

                TYPE_MARKER -> PendingCelebration.Marker(
                    marker = LapMarker.valueOf(fields[1]),
                    lapNumber = fields[2].toInt(),
                    totalDistanceMeters = fields[3].toLong(),
                )

                TYPE_LAP -> PendingCelebration.Lap(
                    lapNumber = fields[1].toInt(),
                    totalDistanceMeters = fields[2].toLong(),
                )

                else -> null
            }
        }.getOrNull()
    }
}
