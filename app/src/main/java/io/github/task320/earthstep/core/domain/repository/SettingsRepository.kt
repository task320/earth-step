package io.github.task320.earthstep.core.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * ユーザー設定とアプリ状態の保存(DataStore Preferences / P1-7)。
 *
 * 距離などのゲームデータは Room 側に持つ。ここに置くのは
 * 「オンボーディングを終えたか」のような、失っても再取得できる軽い状態のみ。
 */
interface SettingsRepository {

    /** オンボーディング(P3-8)を完了したか。 */
    val onboardingCompleted: Flow<Boolean>

    /** 計測を有効にしているか。ユーザーが設定画面で切り替える。 */
    val measurementEnabled: Flow<Boolean>

    /** バッテリー最適化除外の案内(P3-7)を提示済みか。 */
    val batteryOptimizationPromptShown: Flow<Boolean>

    suspend fun setOnboardingCompleted(completed: Boolean)

    suspend fun setMeasurementEnabled(enabled: Boolean)

    suspend fun setBatteryOptimizationPromptShown(shown: Boolean)
}
