package io.github.task320.earthstep.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.common.format.DistanceFormatter
import io.github.task320.earthstep.core.designsystem.component.PixelPanel
import io.github.task320.earthstep.core.designsystem.component.PixelTextButton
import io.github.task320.earthstep.core.designsystem.theme.PixelDimens
import io.github.task320.earthstep.core.designsystem.theme.PixelPalette
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Googleサインイン(P7-6)とDriveへの同期(手動: P7-7、自動: P7-8)。 */
@Composable
internal fun CloudSyncPanel(
    signedInAccount: GoogleAccount?,
    signInFailed: Boolean,
    driveSyncMessage: DriveSyncMessage?,
    lastSyncedAt: Instant?,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onSync: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PixelPanel(modifier = modifier) {
        Text(
            text = stringResource(R.string.settings_cloud_sync_title),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = stringResource(R.string.settings_cloud_sync_note),
            style = MaterialTheme.typography.labelSmall,
            color = PixelPalette.Mist,
        )
        if (signedInAccount != null) {
            Text(
                text = stringResource(
                    R.string.settings_cloud_sync_signed_in_as,
                    signedInAccount.email ?: signedInAccount.displayName.orEmpty(),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = PixelPalette.Green,
            )
            Text(
                text = lastSyncedAt?.let { stringResource(R.string.settings_cloud_sync_last_synced_at, formatDate(it)) }
                    ?: stringResource(R.string.settings_cloud_sync_never_synced),
                style = MaterialTheme.typography.labelSmall,
                color = PixelPalette.Mist,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(PixelDimens.SpaceSmall),
            ) {
                PixelTextButton(text = stringResource(R.string.settings_cloud_sync_action), onClick = onSync)
                PixelTextButton(text = stringResource(R.string.settings_cloud_sync_signout), onClick = onSignOut)
            }
            driveSyncMessage?.let { message ->
                Text(
                    text = message.text(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (message is DriveSyncMessage.Failed) PixelPalette.Rose else PixelPalette.Green,
                )
            }
        } else {
            PixelTextButton(text = stringResource(R.string.settings_cloud_sync_signin), onClick = onSignIn)
            if (signInFailed) {
                Text(
                    text = stringResource(R.string.settings_cloud_sync_signin_failed),
                    style = MaterialTheme.typography.bodySmall,
                    color = PixelPalette.Rose,
                )
            }
        }
    }
}

@Composable
private fun DriveSyncMessage.text(): String = when (this) {
    is DriveSyncMessage.Synced -> stringResource(
        R.string.settings_cloud_sync_synced,
        DistanceFormatter.formatDistance(addedMeters),
    )

    DriveSyncMessage.Failed -> stringResource(R.string.settings_cloud_sync_failed)
}

private fun formatDate(instant: Instant): String = DATE_FORMATTER.format(instant.atZone(ZoneId.systemDefault()))

private val DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm")
