package io.github.task320.earthstep.navigation

/**
 * 画面遷移先。
 * P5-15 でマップ・達成記録・設定を追加し、通知からのディープリンクもここへ集約する。
 */
enum class EarthStepDestination(val route: String) {
    HOME("home"),
}
