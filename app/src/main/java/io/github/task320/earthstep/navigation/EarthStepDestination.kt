package io.github.task320.earthstep.navigation

import io.github.task320.earthstep.R

/**
 * 画面遷移先(P5-15)。
 *
 * アイコンはドット絵素材(P6-4)。
 */
enum class EarthStepDestination(val route: String, val labelRes: Int) {
    ONBOARDING("onboarding", R.string.nav_home),
    HOME("home", R.string.nav_home),
    MAP("map", R.string.nav_map),
    COLLECTION("collection", R.string.nav_collection),
    SETTINGS("settings", R.string.nav_settings),
    ;

    companion object {
        /** 下部タブに並べる画面。オンボーディングは含めない。 */
        val tabs: List<EarthStepDestination> = listOf(HOME, MAP, COLLECTION, SETTINGS)
    }
}

fun EarthStepDestination.iconRes(): Int = when (this) {
    EarthStepDestination.MAP -> R.drawable.ic_tab_map
    EarthStepDestination.COLLECTION -> R.drawable.ic_tab_records
    EarthStepDestination.SETTINGS -> R.drawable.ic_tab_settings
    else -> R.drawable.ic_tab_home
}
