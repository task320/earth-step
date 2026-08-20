package io.github.task320.earthstep.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.designsystem.theme.EarthStepTheme
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.feature.permission.PermissionIntents

/**
 * オンボーディング(仕様7.4 / P3-8)。
 *
 * 1枚につき「説明 → その場でリクエスト」を1セットにする。
 * どの枚でもスキップできるようにしてあるのは、権限を拒否しても縮退して動く設計だから
 * (位置情報が無ければ計測できないが、その状態はホームで警告として出す / P3-9)。
 */
@Composable
fun OnboardingRoute(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val locationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { viewModel.next() }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.next() }

    OnboardingScreen(
        uiState = uiState,
        modifier = modifier,
        onSkip = { viewModel.next() },
        onFinish = { viewModel.complete(onFinished) },
        onPrimaryAction = { step ->
            when (step) {
                OnboardingStep.RULES -> viewModel.next()

                OnboardingStep.LOCATION -> locationLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )

                OnboardingStep.BACKGROUND_LOCATION ->
                    if (PermissionRequirements.canRequestBackgroundLocationDirectly(Build.VERSION.SDK_INT)) {
                        singlePermissionLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    } else {
                        // Android 11 以降はダイアログに「常に許可」が出ないため設定画面へ送る。
                        PermissionIntents.openAppSettings(context)
                    }

                OnboardingStep.NOTIFICATION_AND_BATTERY -> {
                    if (Build.VERSION.SDK_INT >= PermissionRequirements.SDK_POST_NOTIFICATIONS) {
                        singlePermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    PermissionIntents.requestIgnoreBatteryOptimizations(context)
                }
            }
        },
    )
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onPrimaryAction: (OnboardingStep) -> Unit,
    onSkip: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val step = uiState.currentStep ?: return
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = uiState.progressLabel,
                style = MaterialTheme.typography.labelMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = stringResource(step.titleRes()),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(modifier = Modifier.padding(8.dp))
            Text(
                text = stringResource(step.bodyRes()),
                style = MaterialTheme.typography.bodyMedium,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Button(
                    onClick = { onPrimaryAction(step) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = stringResource(step.primaryActionRes(uiState.permissionState)))
                }
                // 最後の1枚だけは「あとで」ではなく「はじめる」で本編へ進む。
                TextButton(onClick = if (uiState.isLastStep) onFinish else onSkip) {
                    Text(
                        text = stringResource(
                            if (uiState.isLastStep) R.string.onboarding_finish else R.string.onboarding_skip,
                        ),
                    )
                }
            }
        }
    }
}

private fun OnboardingStep.titleRes(): Int = when (this) {
    OnboardingStep.RULES -> R.string.onboarding_rules_title
    OnboardingStep.LOCATION -> R.string.onboarding_location_title
    OnboardingStep.BACKGROUND_LOCATION -> R.string.onboarding_background_location_title
    OnboardingStep.NOTIFICATION_AND_BATTERY -> R.string.onboarding_notification_title
}

private fun OnboardingStep.bodyRes(): Int = when (this) {
    OnboardingStep.RULES -> R.string.onboarding_rules_body
    OnboardingStep.LOCATION -> R.string.onboarding_location_body
    OnboardingStep.BACKGROUND_LOCATION -> R.string.onboarding_background_location_body
    OnboardingStep.NOTIFICATION_AND_BATTERY -> R.string.onboarding_notification_body
}

private fun OnboardingStep.primaryActionRes(permissionState: PermissionState): Int = when (this) {
    OnboardingStep.RULES -> R.string.onboarding_next

    OnboardingStep.LOCATION ->
        if (permissionState.isGranted(AppPermission.FINE_LOCATION)) {
            R.string.onboarding_next
        } else {
            R.string.onboarding_location_action
        }

    OnboardingStep.BACKGROUND_LOCATION ->
        if (permissionState.isGranted(AppPermission.BACKGROUND_LOCATION)) {
            R.string.onboarding_next
        } else {
            R.string.onboarding_background_location_action
        }

    OnboardingStep.NOTIFICATION_AND_BATTERY -> R.string.onboarding_notification_action
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    EarthStepTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(
                steps = OnboardingSteps.forSdk(PermissionRequirements.SDK_POST_NOTIFICATIONS),
                currentIndex = 1,
            ),
            onPrimaryAction = {},
            onSkip = {},
            onFinish = {},
        )
    }
}
