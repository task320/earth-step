package io.github.task320.earthstep.feature.home

import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.progress.ProgressSummary

/**
 * ホーム画面の状態。
 * 見た目の作り込み(ドット絵化・進捗バー)は P5-3 で行う。
 */
data class HomeUiState(
    val progress: ProgressSummary = ProgressSummary(),
    val measuring: Boolean = false,
    val permissionState: PermissionState = PermissionState(),
    val versionName: String = "",
) {
    companion object {
        fun initial(versionName: String) = HomeUiState(versionName = versionName)
    }
}
