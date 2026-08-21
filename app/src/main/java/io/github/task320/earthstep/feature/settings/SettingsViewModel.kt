package io.github.task320.earthstep.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.data.backup.BackupFileStore
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.repository.DriveSyncStateRepository
import io.github.task320.earthstep.core.domain.repository.GoogleAuthRepository
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import io.github.task320.earthstep.core.domain.usecase.ExportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportResult
import io.github.task320.earthstep.core.domain.usecase.ResetProgressUseCase
import io.github.task320.earthstep.core.domain.usecase.SyncResult
import io.github.task320.earthstep.core.domain.usecase.SyncWithDriveUseCase
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val measurementEnabled: Boolean = true,
    val strideLengthCm: Double = 0.0,
    val totalDistanceMeters: Long = 0L,
    val permissionState: PermissionState = PermissionState(),
    val signedInAccount: GoogleAccount? = null,
    val lastSyncedAt: Instant? = null,
)

/**
 * バックアップ操作の結果(P7-2 / P7-3)。
 *
 * 取り込みは「増えた距離」を必ず伝える。同じファイルを二度読み込んでも
 * 増えないことが目に見えれば、二重計上の不安を持たずに済む。
 */
sealed interface BackupMessage {
    data object Exported : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val addedMeters: Long) : BackupMessage
    data class ImportUnsupportedVersion(val version: Int) : BackupMessage
    data object ImportFailed : BackupMessage
}

/** Drive同期(P7-7)の結果。一時的な表示のみで、「最終同期日時」は [SettingsUiState.lastSyncedAt] 側に永続化する(P7-9)。 */
sealed interface DriveSyncMessage {
    data class Synced(val addedMeters: Long) : DriveSyncMessage
    data object Failed : DriveSyncMessage
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val resetProgress: ResetProgressUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val backupFileStore: BackupFileStore,
    private val permissionChecker: PermissionChecker,
    private val timeSource: AppTimeSource,
    private val googleAuthRepository: GoogleAuthRepository,
    private val syncWithDrive: SyncWithDriveUseCase,
    driveSyncStateRepository: DriveSyncStateRepository,
    progressRepository: ProgressRepository,
) : ViewModel() {

    private val permissionState = MutableStateFlow(permissionChecker.currentState())

    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage.asStateFlow()

    private val _driveSyncMessage = MutableStateFlow<DriveSyncMessage?>(null)
    val driveSyncMessage: StateFlow<DriveSyncMessage?> = _driveSyncMessage.asStateFlow()

    /** 書き出し先を選ぶときに提案するファイル名。 */
    fun suggestedFileName(): String = backupFileStore.suggestedFileName(timeSource.today().toString())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.measurementEnabled,
        progressRepository.lifetimeStats,
        permissionState,
        googleAuthRepository.signedInAccount,
        driveSyncStateRepository.lastSyncedAt,
    ) { enabled, stats, permissions, signedInAccount, lastSyncedAt ->
        SettingsUiState(
            measurementEnabled = enabled,
            strideLengthCm = stats.strideLengthCm,
            totalDistanceMeters = stats.totalDistanceMeters,
            permissionState = permissions,
            signedInAccount = signedInAccount,
            lastSyncedAt = lastSyncedAt,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState(),
    )

    fun refreshPermissions() {
        permissionState.value = permissionChecker.currentState()
    }

    fun setMeasurementEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setMeasurementEnabled(enabled) }
    }

    fun reset() {
        viewModelScope.launch { resetProgress() }
    }

    /** 選ばれた場所へ書き出す(P7-2)。 */
    fun export(uri: Uri) {
        viewModelScope.launch {
            val written = backupFileStore.write(uri, exportBackup())
            _backupMessage.value = if (written) BackupMessage.Exported else BackupMessage.ExportFailed
        }
    }

    /** 選ばれたファイルを読み込み、いまの記録とマージする(P7-3 / P7-4)。 */
    fun import(uri: Uri) {
        viewModelScope.launch {
            val text = backupFileStore.read(uri)
            if (text == null) {
                _backupMessage.value = BackupMessage.ImportFailed
                return@launch
            }
            _backupMessage.value = when (val result = importBackup(text)) {
                is ImportResult.Success -> BackupMessage.Imported(result.addedMeters)
                is ImportResult.UnsupportedVersion ->
                    BackupMessage.ImportUnsupportedVersion(result.version)

                is ImportResult.Malformed -> BackupMessage.ImportFailed
            }
        }
    }

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    /** サインインの結果をアカウント状態へ反映する(P7-6)。実際のサインイン処理は [GoogleSignIn] が担う。 */
    fun onSignedIn(account: GoogleAccount) {
        viewModelScope.launch { googleAuthRepository.setSignedInAccount(account) }
    }

    fun signOut() {
        viewModelScope.launch { googleAuthRepository.setSignedInAccount(null) }
    }

    /** Driveと同期する(P7-7)。認可トークンの取得は [GoogleDriveAuthorization] が担う。 */
    fun sync(accessToken: String) {
        viewModelScope.launch {
            _driveSyncMessage.value = when (val result = syncWithDrive(accessToken)) {
                is SyncResult.Success -> DriveSyncMessage.Synced(result.addedMeters)
                SyncResult.Failed -> DriveSyncMessage.Failed
            }
        }
    }

    fun clearDriveSyncMessage() {
        _driveSyncMessage.value = null
    }

    /** 認可(同意画面を含む)そのものに失敗したときに呼ぶ。実行は [GoogleDriveAuthorization] が担う。 */
    fun onDriveAuthorizationFailed() {
        _driveSyncMessage.value = DriveSyncMessage.Failed
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
