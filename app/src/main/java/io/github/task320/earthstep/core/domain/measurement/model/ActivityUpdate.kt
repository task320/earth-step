package io.github.task320.earthstep.core.domain.measurement.model

/** ActivityRecognition が返す活動の種類(仕様1.1 / P2-18)。 */
enum class UserActivity {
    /** 徒歩・走行。人力移動としてXPの対象になる。 */
    ON_FOOT,

    /** 自転車。XP対象外。 */
    ON_BICYCLE,

    /** 自動車・電車など。XP対象外。 */
    IN_VEHICLE,

    /** 静止。 */
    STILL,

    /** 分類できなかった。 */
    UNKNOWN,
}

/**
 * 活動判定の1件。
 *
 * @param confidence 0〜100 の信頼度。
 */
data class ActivityUpdate(val activity: UserActivity, val confidence: Int, val timestampMillis: Long)

/** `STILL` → `ON_FOOT` のような活動の遷移(P2-19)。 */
data class ActivityTransitionEvent(val activity: UserActivity, val isEnter: Boolean, val timestampMillis: Long)
