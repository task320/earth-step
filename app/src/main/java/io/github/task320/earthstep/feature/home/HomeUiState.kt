package io.github.task320.earthstep.feature.home

/**
 * ホーム画面の状態。
 * 次のマイルストーンまでの距離と進捗率は P4-4 で追加する。
 */
data class HomeUiState(
    val totalDistanceMeters: Long = 0L,
    val todayDistanceMeters: Long = 0L,
    val currentLap: Int = 1,
    val versionName: String = "",
) {
    companion object {
        fun initial(versionName: String) = HomeUiState(versionName = versionName)
    }
}
