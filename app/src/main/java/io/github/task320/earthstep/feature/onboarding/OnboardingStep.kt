package io.github.task320.earthstep.feature.onboarding

import io.github.task320.earthstep.core.domain.permission.PermissionRequirements

/**
 * オンボーディングの1枚(仕様7.4 / P3-8)。
 *
 * 権限のリクエストは「なぜ必要か」を説明した直後に出す。
 * まとめて一度に出すと、何のための権限か分からないまま拒否されやすい。
 */
enum class OnboardingStep {
    /** ゲームのルール説明。権限のリクエストは無い。 */
    RULES,

    /** 位置情報(使用中のみ)の説明とリクエスト。 */
    LOCATION,

    /** 背景位置の説明とリクエスト。Android 10 未満では不要。 */
    BACKGROUND_LOCATION,

    /** 通知とバッテリー最適化の説明。 */
    NOTIFICATION_AND_BATTERY,
}

/** 端末の版数に応じて表示する枚数を決める。 */
object OnboardingSteps {

    fun forSdk(sdkInt: Int): List<OnboardingStep> = buildList {
        add(OnboardingStep.RULES)
        add(OnboardingStep.LOCATION)
        if (sdkInt >= PermissionRequirements.SDK_BACKGROUND_LOCATION) {
            add(OnboardingStep.BACKGROUND_LOCATION)
        }
        add(OnboardingStep.NOTIFICATION_AND_BATTERY)
    }
}
