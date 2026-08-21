package io.github.task320.earthstep.core.domain.repository

/**
 * Google Drive の `appDataFolder`(アプリ専用の隠し領域)への読み書き(P7-7)。
 *
 * ファイルは常に1つだけ保つ(バックアップJSON)。認可トークンの取得(Activity依存)は
 * `feature/settings/GoogleDriveAuthorization` が担い、ここは純粋なHTTP通信のみを行う。
 */
interface DriveSyncRepository {

    /** 保存済みのバックアップJSONを取得する。ファイルが無ければ null。通信に失敗した場合も null。 */
    suspend fun download(accessToken: String): String?

    /** バックアップJSONを書き込む(無ければ新規作成、あれば上書き)。成功したら true。 */
    suspend fun upload(accessToken: String, json: String): Boolean
}
