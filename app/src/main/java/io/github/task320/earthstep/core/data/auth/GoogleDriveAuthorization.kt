package io.github.task320.earthstep.core.data.auth

import android.content.Context
import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.AuthorizationResult
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.tasks.await
import timber.log.Timber

/**
 * Drive `appDataFolder` への認可(P7-7 / P7-8)。
 *
 * サインイン(P7-6、本人確認)とは別の関心事として、Google公式が案内する
 * `AuthorizationClient` を使う(Credential Manager自体はスコープの認可を扱わない)。
 * 既に許可済みならActivityが無くても(バックグラウンドの[io.github.task320.earthstep.core.data.work.DriveSyncWorker]からでも)
 * トークンを取得できるが、未許可で同意画面が要る場合はActivityから同意画面を起動する必要がある。
 * その場合はViewModelを介さずComposable(`SettingsScreen`)から直接呼ぶ。
 */
object GoogleDriveAuthorization {

    private const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"

    sealed interface Outcome {
        data class Authorized(val accessToken: String) : Outcome
        data class NeedsConsent(val request: IntentSenderRequest) : Outcome
        data object Failed : Outcome
    }

    /** 認可を要求する。既に許可済みならそのままトークンを返す。未許可なら同意画面の起動要求を返す。 */
    suspend fun authorize(context: Context): Outcome {
        val request = AuthorizationRequest.builder()
            .setRequestedScopes(listOf(Scope(DRIVE_APPDATA_SCOPE)))
            .build()

        return try {
            Identity.getAuthorizationClient(context).authorize(request).await().toOutcome()
        } catch (e: ApiException) {
            Timber.w(e, "drive authorization failed")
            Outcome.Failed
        }
    }

    /** [Outcome.NeedsConsent] の同意画面から戻ってきたときに呼ぶ。 */
    fun resultFromIntent(context: Context, data: Intent?): Outcome = try {
        Identity.getAuthorizationClient(context).getAuthorizationResultFromIntent(data).toOutcome()
    } catch (e: ApiException) {
        Timber.w(e, "drive authorization consent failed")
        Outcome.Failed
    }

    private fun AuthorizationResult.toOutcome(): Outcome {
        val consentIntent = pendingIntent
        val token = accessToken
        return when {
            hasResolution() && consentIntent != null ->
                Outcome.NeedsConsent(IntentSenderRequest.Builder(consentIntent.intentSender).build())

            token != null -> Outcome.Authorized(token)
            else -> Outcome.Failed
        }
    }
}
