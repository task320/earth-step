package io.github.task320.earthstep.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import io.github.task320.earthstep.core.designsystem.component.PixelImage
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.feature.celebration.CelebrationOverlay
import io.github.task320.earthstep.feature.collection.CollectionRoute
import io.github.task320.earthstep.feature.home.HomeRoute
import io.github.task320.earthstep.feature.map.MapRoute
import io.github.task320.earthstep.feature.onboarding.OnboardingRoute
import io.github.task320.earthstep.feature.settings.SettingsRoute

/**
 * 画面の骨組み(P5-15)。
 *
 * オンボーディング中はタブを出さない。権限の説明の途中で他の画面へ行けると、
 * 説明を読まずに素通りされやすい。
 *
 * 演出([CelebrationOverlay])はタブの上に重ねる。どの画面にいても、
 * 貯まっている演出があれば必ず目に入るようにするため(P5-13)。
 */
@Composable
fun EarthStepNavHost(
    startDestination: EarthStepDestination,
    onOnboardingFinished: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.hierarchy?.firstOrNull()?.route
    val showsBottomBar = currentRoute != EarthStepDestination.ONBOARDING.route

    Scaffold(
        bottomBar = {
            if (showsBottomBar) {
                EarthStepBottomBar(
                    currentRoute = currentRoute,
                    onSelect = { destination -> navController.navigateToTab(destination) },
                )
            }
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = startDestination.route,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable(EarthStepDestination.ONBOARDING.route) {
                    OnboardingRoute(
                        onFinished = {
                            onOnboardingFinished()
                            // オンボーディングへは戻さない。権限の再設定は設定画面から行う。
                            navController.navigate(EarthStepDestination.HOME.route) {
                                popUpTo(EarthStepDestination.ONBOARDING.route) { inclusive = true }
                            }
                        },
                    )
                }
                composable(EarthStepDestination.HOME.route) { HomeRoute() }
                composable(EarthStepDestination.MAP.route) { MapRoute() }
                composable(EarthStepDestination.COLLECTION.route) { CollectionRoute() }
                composable(EarthStepDestination.SETTINGS.route) { SettingsRoute() }
            }

            if (showsBottomBar) {
                CelebrationOverlay()
            }
        }
    }
}

@Composable
private fun EarthStepBottomBar(
    currentRoute: String?,
    onSelect: (EarthStepDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    NavigationBar(modifier = modifier, containerColor = PixelPalette.Deep) {
        EarthStepDestination.tabs.forEach { destination ->
            NavigationBarItem(
                selected = currentRoute == destination.route,
                onClick = { onSelect(destination) },
                icon = {
                    PixelImage(
                        resourceId = destination.iconRes(),
                        contentDescription = stringResource(destination.labelRes),
                        modifier = Modifier.size(PixelDimens.TabIconSize),
                        tint = LocalContentColor.current,
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PixelPalette.Night,
                    selectedTextColor = PixelPalette.Gold,
                    indicatorColor = PixelPalette.Gold,
                    unselectedIconColor = PixelPalette.Mist,
                    unselectedTextColor = PixelPalette.Mist,
                ),
            )
        }
    }
}

/**
 * タブの切り替え。
 * 戻るキーで必ずホームへ戻れるよう、履歴はホームまで畳んで積み直さない。
 */
private fun NavHostController.navigateToTab(destination: EarthStepDestination) {
    navigate(destination.route) {
        popUpTo(EarthStepDestination.HOME.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
