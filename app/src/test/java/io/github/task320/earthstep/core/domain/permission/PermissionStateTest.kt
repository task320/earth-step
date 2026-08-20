package io.github.task320.earthstep.core.domain.permission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P3-9: 権限の欠けかたに応じた縮退の判定。 */
class PermissionStateTest {

    private val modern = PermissionRequirements.requiredOn(sdkInt = 33)

    @Test
    fun `位置情報が無ければ計測できない`() {
        val state = PermissionState(granted = emptySet(), required = modern)

        assertThat(state.canMeasure).isFalse()
        assertThat(state.canMeasureInBackground).isFalse()
        assertThat(state.missing).containsExactlyElementsIn(modern)
    }

    @Test
    fun `位置情報だけあれば前面の計測はできる`() {
        val state = PermissionState(granted = setOf(AppPermission.FINE_LOCATION), required = modern)

        assertThat(state.canMeasure).isTrue()
        assertThat(state.canMeasureInBackground).isFalse()
        assertThat(state.canNotify).isFalse()
        assertThat(state.canDetectActivity).isFalse()
    }

    @Test
    fun `背景位置が揃えば常駐計測ができる`() {
        val state = PermissionState(
            granted = setOf(AppPermission.FINE_LOCATION, AppPermission.BACKGROUND_LOCATION),
            required = modern,
        )

        assertThat(state.canMeasureInBackground).isTrue()
    }

    @Test
    fun `そのバージョンで不要な権限は許可済み扱いになる`() {
        // Android 8 では通知も活動検知も実行時権限ではない。
        val legacy = PermissionRequirements.requiredOn(sdkInt = 26)
        val state = PermissionState(granted = setOf(AppPermission.FINE_LOCATION), required = legacy)

        assertThat(state.canNotify).isTrue()
        assertThat(state.canDetectActivity).isTrue()
        assertThat(state.canMeasureInBackground).isTrue()
        assertThat(state.missing).isEmpty()
    }

    @Test
    fun `すべて揃いバッテリー最適化も外れていれば警告は出ない`() {
        val state = PermissionState(
            granted = modern,
            required = modern,
            batteryOptimizationIgnored = true,
        )

        assertThat(state.hasWarning).isFalse()
    }

    @Test
    fun `バッテリー最適化の対象なら警告が残る`() {
        val state = PermissionState(granted = modern, required = modern, batteryOptimizationIgnored = false)

        assertThat(state.hasWarning).isTrue()
    }
}
