package io.github.task320.earthstep.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.task320.earthstep.feature.home.HomeRoute
import io.github.task320.earthstep.feature.onboarding.OnboardingRoute

@Composable
fun EarthStepNavHost(
    startDestination: EarthStepDestination,
    onOnboardingFinished: () -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = startDestination.route,
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
        composable(EarthStepDestination.HOME.route) {
            HomeRoute()
        }
    }
}
