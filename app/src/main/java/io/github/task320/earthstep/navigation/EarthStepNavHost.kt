package io.github.task320.earthstep.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.task320.earthstep.feature.home.HomeRoute

@Composable
fun EarthStepNavHost(navController: NavHostController = rememberNavController()) {
    NavHost(
        navController = navController,
        startDestination = EarthStepDestination.HOME.route,
    ) {
        composable(EarthStepDestination.HOME.route) {
            HomeRoute()
        }
    }
}
