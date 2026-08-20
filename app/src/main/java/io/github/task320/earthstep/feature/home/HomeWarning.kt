package io.github.task320.earthstep.feature.home

import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionState

/**
 * 権限が欠けている状態の警告(P3-9)。
 *
 * どれも「アプリは動くが、この機能が効かない」という縮退の説明にする。
 * 並び順は計測への影響が大きい順で、位置情報が無い状態が最も重い。
 */
enum class HomeWarning(val messageRes: Int) {
    MISSING_LOCATION(R.string.home_permission_missing_location),
    MISSING_BACKGROUND_LOCATION(R.string.home_permission_missing_background_location),
    MISSING_ACTIVITY_RECOGNITION(R.string.home_permission_missing_activity_recognition),
    MISSING_NOTIFICATION(R.string.home_permission_missing_notification),
    BATTERY_OPTIMIZATION(R.string.home_permission_battery_optimization),
    ;

    companion object {
        fun from(state: PermissionState): List<HomeWarning> = buildList {
            if (!state.canMeasure) add(MISSING_LOCATION)
            if (state.canMeasure && !state.canMeasureInBackground) add(MISSING_BACKGROUND_LOCATION)
            if (!state.canDetectActivity) add(MISSING_ACTIVITY_RECOGNITION)
            if (!state.canNotify) add(MISSING_NOTIFICATION)
            if (AppPermission.FINE_LOCATION in state.granted && !state.batteryOptimizationIgnored) {
                add(BATTERY_OPTIMIZATION)
            }
        }
    }
}
