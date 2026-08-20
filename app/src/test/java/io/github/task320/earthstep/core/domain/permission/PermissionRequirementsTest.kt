package io.github.task320.earthstep.core.domain.permission

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P3-2 / P3-3 / P3-4: 版数ごとに必要な権限が変わることの検証。 */
class PermissionRequirementsTest {

    @Test
    fun `Android 8では位置情報だけが必要`() {
        val required = PermissionRequirements.requiredOn(sdkInt = 26)

        assertThat(required).containsExactly(AppPermission.FINE_LOCATION)
    }

    @Test
    fun `Android 10で背景位置と活動検知が加わる`() {
        val required = PermissionRequirements.requiredOn(sdkInt = 29)

        assertThat(required).containsExactly(
            AppPermission.FINE_LOCATION,
            AppPermission.BACKGROUND_LOCATION,
            AppPermission.ACTIVITY_RECOGNITION,
        )
    }

    @Test
    fun `Android 13で通知権限が加わる`() {
        val required = PermissionRequirements.requiredOn(sdkInt = 33)

        assertThat(required).containsExactly(
            AppPermission.FINE_LOCATION,
            AppPermission.BACKGROUND_LOCATION,
            AppPermission.ACTIVITY_RECOGNITION,
            AppPermission.POST_NOTIFICATIONS,
        )
    }

    @Test
    fun `背景位置をダイアログで直接聞けるのはAndroid 10だけ`() {
        // 仕様7.1-2: Android 11 以降は設定画面へ誘導するしかない。
        assertThat(PermissionRequirements.canRequestBackgroundLocationDirectly(26)).isFalse()
        assertThat(PermissionRequirements.canRequestBackgroundLocationDirectly(29)).isTrue()
        assertThat(PermissionRequirements.canRequestBackgroundLocationDirectly(30)).isFalse()
        assertThat(PermissionRequirements.canRequestBackgroundLocationDirectly(35)).isFalse()
    }
}
