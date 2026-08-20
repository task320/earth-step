package io.github.task320.earthstep.feature.onboarding

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/** P3-8: 版数に応じた枚数(仕様7.4 の「3〜4枚」)。 */
class OnboardingStepsTest {

    @Test
    fun `Android 8では背景位置の説明を省いて3枚になる`() {
        val steps = OnboardingSteps.forSdk(sdkInt = 26)

        assertThat(steps).containsExactly(
            OnboardingStep.RULES,
            OnboardingStep.LOCATION,
            OnboardingStep.NOTIFICATION_AND_BATTERY,
        ).inOrder()
    }

    @Test
    fun `Android 10以降は背景位置の説明を挟んで4枚になる`() {
        val steps = OnboardingSteps.forSdk(sdkInt = 33)

        assertThat(steps).containsExactly(
            OnboardingStep.RULES,
            OnboardingStep.LOCATION,
            OnboardingStep.BACKGROUND_LOCATION,
            OnboardingStep.NOTIFICATION_AND_BATTERY,
        ).inOrder()
    }

    @Test
    fun `権限のリクエストは必ず説明の後に来る`() {
        // 仕様7.4: 各ステップの直前に「なぜ必要か」を挟む。
        // ルール説明が先頭にあり、位置情報より前に来ることを保証する。
        val steps = OnboardingSteps.forSdk(sdkInt = 33)

        assertThat(steps.first()).isEqualTo(OnboardingStep.RULES)
        assertThat(steps.indexOf(OnboardingStep.LOCATION))
            .isLessThan(steps.indexOf(OnboardingStep.BACKGROUND_LOCATION))
    }
}
