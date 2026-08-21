package io.github.task320.earthstep.core.domain.repository

import io.github.task320.earthstep.core.domain.model.GoogleAccount
import kotlinx.coroutines.flow.Flow

/**
 * サインイン状態の保存(P7-6)。
 *
 * サインインの実行そのもの(Credential Manager呼び出し)はActivity Contextを要るAndroid依存の処理のため、
 * `feature/settings/GoogleSignIn` が担う。ここでは結果の保存・読み出しだけを担当する。
 */
interface GoogleAuthRepository {

    /** サインイン中のアカウント。未サインインなら null。 */
    val signedInAccount: Flow<GoogleAccount?>

    suspend fun setSignedInAccount(account: GoogleAccount?)
}
