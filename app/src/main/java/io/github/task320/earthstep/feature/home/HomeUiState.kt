package io.github.task320.earthstep.feature.home

/**
 * ホーム画面の状態。
 * P1-7 で累計距離・当日距離・周回数・次のマイルストーンまでの距離を実データで埋める。
 */
data class HomeUiState(
    val totalDistanceMeters: Long,
    val versionName: String,
)
