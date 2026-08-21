package io.github.task320.earthstep.core.domain.model

/**
 * サインイン済みGoogleアカウント(P7-6)。
 *
 * 表示・同期先の識別に使う情報のみ持つ。Drive同期(P7-7)に使う認可トークンは別途扱う。
 *
 * @param id Googleアカウントの一意なID(`GoogleIdTokenCredential.uniqueId`)。
 */
data class GoogleAccount(val id: String, val email: String?, val displayName: String?)
