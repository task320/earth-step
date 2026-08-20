package io.github.task320.earthstep.feature.onboarding

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** P8-7: オンボーディング(P3-8)の表示と進み方。 */
@RunWith(RobolectricTestRunner::class)
class OnboardingScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val steps = OnboardingSteps.forSdk(sdkInt = 33)

    @Test
    fun `最初の1枚はゲームのルールを説明する`() {
        setContent(currentIndex = 0)

        composeRule.onNodeWithText(string(R.string.onboarding_rules_title)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_rules_body)).assertIsDisplayed()
    }

    @Test
    fun `何枚目かを出す`() {
        setContent(currentIndex = 1)

        composeRule.onNodeWithText("2 / ${steps.size}").assertIsDisplayed()
    }

    @Test
    fun `位置情報の枚では理由を説明してから許可を求める`() {
        // 仕様7.4: 各リクエストの直前に「なぜ必要か」を挟む。
        setContent(currentIndex = steps.indexOf(OnboardingStep.LOCATION))

        composeRule.onNodeWithText(string(R.string.onboarding_location_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_location_action)).assertIsDisplayed()
    }

    @Test
    fun `許可済みならボタンは次へに変わる`() {
        setContent(
            currentIndex = steps.indexOf(OnboardingStep.LOCATION),
            permissionState = PermissionState(granted = setOf(AppPermission.FINE_LOCATION)),
        )

        composeRule.onNodeWithText(string(R.string.onboarding_next)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_location_action)).assertDoesNotExist()
    }

    @Test
    fun `背景位置の枚では設定画面への誘導を出す`() {
        setContent(currentIndex = steps.indexOf(OnboardingStep.BACKGROUND_LOCATION))

        composeRule.onNodeWithText(string(R.string.onboarding_background_location_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_background_location_action)).assertIsDisplayed()
    }

    @Test
    fun `主ボタンを押すとその枚の操作が走る`() {
        var acted: OnboardingStep? = null
        setContent(currentIndex = 0, onPrimaryAction = { acted = it })

        composeRule.onNodeWithText(string(R.string.onboarding_next)).performClick()

        assertThat(acted).isEqualTo(OnboardingStep.RULES)
    }

    @Test
    fun `途中の枚はあとで設定できる`() {
        // 権限を拒否しても縮退して動く設計なので、どの枚もスキップできる。
        var skipped = false
        setContent(currentIndex = 0, onSkip = { skipped = true })

        composeRule.onNodeWithText(string(R.string.onboarding_skip)).performClick()

        assertThat(skipped).isTrue()
    }

    @Test
    fun `最後の1枚だけ本編へ進むボタンになる`() {
        var finished = false
        setContent(currentIndex = steps.lastIndex, onFinish = { finished = true })

        composeRule.onNodeWithText(string(R.string.onboarding_skip)).assertDoesNotExist()
        composeRule.onNodeWithText(string(R.string.onboarding_finish)).performClick()

        assertThat(finished).isTrue()
    }

    @Test
    fun `最後の1枚では通知とバッテリーの設定を促す`() {
        setContent(currentIndex = steps.lastIndex)

        composeRule.onNodeWithText(string(R.string.onboarding_notification_body)).assertIsDisplayed()
        composeRule.onNodeWithText(string(R.string.onboarding_notification_action)).assertIsDisplayed()
    }

    private fun setContent(
        currentIndex: Int,
        permissionState: PermissionState = PermissionState(
            required = PermissionRequirements.requiredOn(sdkInt = 33),
        ),
        onPrimaryAction: (OnboardingStep) -> Unit = {},
        onSkip: () -> Unit = {},
        onFinish: () -> Unit = {},
    ) {
        composeRule.setContent {
            EarthStepTheme {
                OnboardingScreen(
                    uiState = OnboardingUiState(
                        steps = steps,
                        currentIndex = currentIndex,
                        permissionState = permissionState,
                    ),
                    onPrimaryAction = onPrimaryAction,
                    onSkip = onSkip,
                    onFinish = onFinish,
                )
            }
        }
    }

    private fun string(resId: Int, vararg args: Any): String = composeRule.activity.getString(resId, *args)
}
