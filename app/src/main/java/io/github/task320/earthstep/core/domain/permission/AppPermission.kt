package io.github.task320.earthstep.core.domain.permission

/**
 * アプリが必要とする権限(仕様7.1)。
 *
 * どれが必要かは Android のバージョンで変わるため、判定は [PermissionRequirements] に集約する。
 */
enum class AppPermission {
    /** 位置情報(使用中のみ)。これが無いと距離計測そのものが動かない。 */
    FINE_LOCATION,

    /** 背景位置。無くても前面では計測できるが、常駐計測には必須。 */
    BACKGROUND_LOCATION,

    /** 活動検知。無いと人力移動の判定ができず、乗り物を弾けない。 */
    ACTIVITY_RECOGNITION,

    /** 通知。無いとマイルストーン達成通知と常駐通知が出せない。 */
    POST_NOTIFICATIONS,
}
