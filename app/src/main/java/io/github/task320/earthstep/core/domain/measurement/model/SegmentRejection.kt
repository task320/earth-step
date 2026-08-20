package io.github.task320.earthstep.core.domain.measurement.model

/**
 * 区間を距離加算の対象外にした理由。
 * 仕様1.5の「ログへ残す」を満たすため、破棄した理由を必ず持ち回る。
 */
enum class SegmentRejection {
    /** 精度が閾値より粗い(P2-3)。 */
    LOW_ACCURACY,

    /** モック位置プロバイダ由来(P2-6)。 */
    MOCK_PROVIDER,

    /** 区間速度が上限を超えた(P2-4)。 */
    SPEED_TOO_HIGH,

    /** 前の位置からの間隔が開きすぎた。歩数フォールバックの担当区間(P2-15)。 */
    GAP_TOO_LONG,

    /** 時刻が進んでいない(重複・逆行サンプル)。 */
    NON_MONOTONIC_TIME,

    /** 停止中(仕様1.4)。 */
    STOPPED,

    /** 起点が無い(計測開始直後、またはリセット直後)。 */
    NO_ANCHOR,

    /** 人力移動と判定されていない(自転車・乗り物・不明)。仕様1.1 / P2-18。 */
    NOT_ON_FOOT,

    /** 歩数モード中に届いた位置。復帰判定にのみ使い、距離にはしない(P2-15)。 */
    STEP_FALLBACK_ACTIVE,
}
