package io.github.task320.earthstep.feature.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.task320.earthstep.core.common.time.AppTimeSource
import io.github.task320.earthstep.core.data.backup.BackupFileStore
import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionState
import io.github.task320.earthstep.core.domain.repository.ProgressRepository
import io.github.task320.earthstep.core.domain.repository.SettingsRepository
import io.github.task320.earthstep.core.domain.usecase.ExportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportBackupUseCase
import io.github.task320.earthstep.core.domain.usecase.ImportResult
import io.github.task320.earthstep.core.domain.usecase.ResetProgressUseCase
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

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val resetProgress: ResetProgressUseCase,
    private val exportBackup: ExportBackupUseCase,
    private val importBackup: ImportBackupUseCase,
    private val backupFileStore: BackupFileStore,
    private val permissionChecker: PermissionChecker,
    private val timeSource: AppTimeSource,
    progressRepository: ProgressRepository,
) : ViewModel() {

    private val permissionState = MutableStateFlow(permissionChecker.currentState())

    private val _backupMessage = MutableStateFlow<BackupMessage?>(null)
    val backupMessage: StateFlow<BackupMessage?> = _backupMessage.asStateFlow()

    /** 書き出し先を選ぶときに提案するファイル名。 */
    fun suggestedFileName(): String = backupFileStore.suggestedFileName(timeSource.today().toString())

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.measurementEnabled,
        progressRepository.lifetimeStats,
        permissionState,
    ) { enabled, stats, permissions ->
        SettingsUiState(
            measurementEnabled = enabled,
            strideLengthCm = stats.strideLengthCm,
            totalDistanceMeters = stats.totalDistanceMeters,
            permissionState = permissions,
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

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
