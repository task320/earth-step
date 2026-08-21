package io.github.task320.earthstep.feature.settings

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.ClearCredentialException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import io.github.task320.earthstep.R
import io.github.task320.earthstep.core.domain.model.GoogleAccount
import timber.log.Timber

/**
 * Googleサインイン(P7-6)。
 *
 * Credential Manager は Activity を伴う Context でしか正しく動かないため、
 * [io.github.task320.earthstep.feature.permission.PermissionIntents] と同じく
 * ViewModel を介さず Composable から直接呼ぶ。
 */
object GoogleSignIn {

    /** サインインを開始し、成功したアカウントを返す。キャンセル・失敗時は null。 */
    suspend fun signIn(context: Context): GoogleAccount? {
        val option = GetGoogleIdOption.Builder()
            .setFilterByAuthorizedAccounts(false)
            .setServerClientId(context.getString(R.string.google_web_client_id))
            .build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()

        val credential = try {
            CredentialManager.create(context).getCredential(context, request).credential
        } catch (e: NoCredentialException) {
            // 端末にGoogleアカウントが1つも登録されていない。
            Timber.w(e, "no google account available on this device")
            return null
        } catch (e: GetCredentialException) {
            Timber.w(e, "google sign-in failed")
            return null
        }

        if (credential !is CustomCredential ||
            credential.type != GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            Timber.w("unexpected credential type from google sign-in")
            return null
        }

        return try {
            val token = GoogleIdTokenCredential.createFrom(credential.data)
            GoogleAccount(id = token.uniqueId, email = token.email, displayName = token.displayName)
        } catch (e: GoogleIdTokenParsingException) {
            Timber.w(e, "google id token parsing failed")
            null
        }
    }

    /** サインアウト。次回サインイン時に毎回アカウント選択をやり直させる。 */
    suspend fun signOut(context: Context) {
        try {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        } catch (e: ClearCredentialException) {
            Timber.w(e, "google sign-out failed")
        }
    }
}
