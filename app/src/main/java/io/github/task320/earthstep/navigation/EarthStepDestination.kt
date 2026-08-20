package io.github.task320.earthstep.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.task320.earthstep.R

/**
 * 画面遷移先(P5-15)。
 *
 * アイコンは Material の既定を使う。ドット絵のアイコン素材は P6-4 で差し替える。
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

@Composable
fun EarthStepDestination.icon(): ImageVector = when (this) {
    EarthStepDestination.MAP -> Icons.Filled.Place
    EarthStepDestination.COLLECTION -> Icons.Filled.List
    EarthStepDestination.SETTINGS -> Icons.Filled.Settings
    else -> Icons.Filled.Home
}
