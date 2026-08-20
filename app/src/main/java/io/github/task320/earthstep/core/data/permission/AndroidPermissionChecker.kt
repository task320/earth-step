package io.github.task320.earthstep.core.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.domain.permission.AppPermission
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionRequirements
import io.github.task320.earthstep.core.domain.permission.PermissionState
import javax.inject.Inject
import javax.inject.Singleton

/** `checkSelfPermission` で許可状態を読む [PermissionChecker]。 */
@Singleton
class AndroidPermissionChecker @Inject constructor(@ApplicationContext private val context: Context) :
    PermissionChecker {

    override fun currentState(): PermissionState {
        val required = PermissionRequirements.requiredOn(Build.VERSION.SDK_INT)
        return PermissionState(
            granted = required.filterTo(mutableSetOf()) { it.isGranted() },
            required = required,
            batteryOptimizationIgnored = isBatteryOptimizationIgnored(),
        )
    }

    private fun AppPermission.isGranted(): Boolean {
        val manifestPermission = toManifestPermission() ?: return true
        return ContextCompat.checkSelfPermission(context, manifestPermission) ==
            PackageManager.PERMISSION_GRANTED
    }

    /**
     * 対応する Android の権限名。
     * その版数に存在しない権限は null を返し、常に許可済みとして扱う。
     */
    private fun AppPermission.toManifestPermission(): String? = when (this) {
        AppPermission.FINE_LOCATION -> Manifest.permission.ACCESS_FINE_LOCATION
        AppPermission.BACKGROUND_LOCATION ->
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                .takeIf { Build.VERSION.SDK_INT >= PermissionRequirements.SDK_BACKGROUND_LOCATION }

        AppPermission.ACTIVITY_RECOGNITION ->
            Manifest.permission.ACTIVITY_RECOGNITION
                .takeIf { Build.VERSION.SDK_INT >= PermissionRequirements.SDK_BACKGROUND_LOCATION }

        AppPermission.POST_NOTIFICATIONS ->
            Manifest.permission.POST_NOTIFICATIONS
                .takeIf { Build.VERSION.SDK_INT >= PermissionRequirements.SDK_POST_NOTIFICATIONS }
    }

    private fun isBatteryOptimizationIgnored(): Boolean {
        val powerManager = context.getSystemService(PowerManager::class.java) ?: return false
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }
}
