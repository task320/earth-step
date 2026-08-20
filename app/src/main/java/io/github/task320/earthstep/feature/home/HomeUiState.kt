package io.github.task320.earthstep.feature.home

import io.github.task320.earthstep.core.domain.permission.PermissionState

/**
 * ホーム画面の状態。
 * 次のマイルストーンまでの距離と進捗率は P4-4 で追加する。
 */
data class HomeUiState(
    val totalDistanceMeters: Long = 0L,
    val todayDistanceMeters: Long = 0L,
    val currentLap: Int = 1,
    val measuring: Boolean = false,
    val permissionState: PermissionState = PermissionState(),
    val versionName: String = "",
) {
    companion object {
        fun initial(versionName: String) = HomeUiState(versionName = versionName)
    }
}
