package io.github.task320.earthstep.feature.home

import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import org.junit.Test

/** P3-9: ホームに出す警告の組み立て。 */
class HomeWarningTest {

    private val required = PermissionRequirements.requiredOn(sdkInt = 33)

    @Test
    fun `位置情報が無いときは最初に計測不可を出す`() {
        val warnings = HomeWarning.from(PermissionState(required = required))

        assertThat(warnings.first()).isEqualTo(HomeWarning.MISSING_LOCATION)
    }

    @Test
    fun `位置情報が無いときは背景位置の警告を重ねない`() {
        // まず「測れない」を直してもらう。同時に何行も出しても読まれない。
        val warnings = HomeWarning.from(PermissionState(required = required))

        assertThat(warnings).doesNotContain(HomeWarning.MISSING_BACKGROUND_LOCATION)
    }

    @Test
    fun `位置情報だけ許可された状態では背景位置を促す`() {
        val warnings = HomeWarning.from(
            PermissionState(granted = setOf(AppPermission.FINE_LOCATION), required = required),
        )

        assertThat(warnings).containsExactly(
            HomeWarning.MISSING_BACKGROUND_LOCATION,
            HomeWarning.MISSING_ACTIVITY_RECOGNITION,
            HomeWarning.MISSING_NOTIFICATION,
            HomeWarning.BATTERY_OPTIMIZATION,
        ).inOrder()
    }

    @Test
    fun `すべて揃っていれば警告は空になる`() {
        val warnings = HomeWarning.from(
            PermissionState(granted = required, required = required, batteryOptimizationIgnored = true),
        )

        assertThat(warnings).isEmpty()
    }

    @Test
    fun `バッテリー最適化だけが残ることもある`() {
        val warnings = HomeWarning.from(
            PermissionState(granted = required, required = required, batteryOptimizationIgnored = false),
        )

        assertThat(warnings).containsExactly(HomeWarning.BATTERY_OPTIMIZATION)
    }
}
