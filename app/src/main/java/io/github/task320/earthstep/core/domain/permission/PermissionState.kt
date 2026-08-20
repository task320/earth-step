package io.github.task320.earthstep.core.domain.permission

/**
 * 権限の現在の状態(P3-9)。
 *
 * @param granted 許可済みの権限。
 * @param required この端末で必要な権限([PermissionRequirements.requiredOn])。
 * @param batteryOptimizationIgnored バッテリー最適化の対象外になっているか(仕様7.2)。
 */
data class PermissionState(
    val granted: Set<AppPermission> = emptySet(),
    val required: Set<AppPermission> = emptySet(),
    val batteryOptimizationIgnored: Boolean = false,
) {

    /** まだ許可されていない権限。 */
    val missing: Set<AppPermission> get() = required - granted

    /** 前面での計測ができるか。位置情報が無ければ何も測れない。 */
    val canMeasure: Boolean get() = AppPermission.FINE_LOCATION in granted

    /** 常駐計測ができるか。背景位置が要らない端末では [canMeasure] と同じ。 */
    val canMeasureInBackground: Boolean
        get() = canMeasure &&
            (AppPermission.BACKGROUND_LOCATION !in required || AppPermission.BACKGROUND_LOCATION in granted)

    /** 通知を出せるか。出せなくても計測は続ける(P3-4 の縮退動作)。 */
    val canNotify: Boolean
        get() = AppPermission.POST_NOTIFICATIONS !in required || AppPermission.POST_NOTIFICATIONS in granted

    /** 活動判定を使えるか。使えない場合は乗り物の除外が効かない(仕様1.1)。 */
    val canDetectActivity: Boolean
        get() = AppPermission.ACTIVITY_RECOGNITION !in required || AppPermission.ACTIVITY_RECOGNITION in granted

    /** ホームで警告を出すべきか(P3-9)。 */
    val hasWarning: Boolean get() = missing.isNotEmpty() || !batteryOptimizationIgnored

    fun isGranted(permission: AppPermission): Boolean = permission in granted
}
