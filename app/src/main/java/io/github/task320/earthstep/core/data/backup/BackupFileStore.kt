package io.github.task320.earthstep.core.data.backup

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.common.di.IoDispatcher
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * SAF で選ばれた場所への読み書き(P7-2 / P7-3)。
 *
 * 保存先はユーザーが選んだ [Uri]。アプリの領域外に置くので、
 * アンインストールしてもバックアップは残る。
 */
@Singleton
class BackupFileStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun write(uri: Uri, text: String): Boolean = withContext(ioDispatcher) {
        try {
            // "wt" は truncate 付き。既存ファイルを選び直したとき、
            // 前の内容が後ろに残らないようにする。
            context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
                stream.write(text.toByteArray())
            } ?: return@withContext false
            true
        } catch (e: IOException) {
            Timber.w(e, "backup export failed")
            false
        } catch (e: SecurityException) {
            Timber.w(e, "no permission for the selected location")
            false
        }
    }

    suspend fun read(uri: Uri): String? = withContext(ioDispatcher) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.readBytes().decodeToString()
            }
        } catch (e: IOException) {
            Timber.w(e, "backup import failed")
            null
        } catch (e: SecurityException) {
            Timber.w(e, "no permission for the selected file")
            null
        }
    }

    /** 書き出しの既定ファイル名。日付を入れて、複数世代を残せるようにする。 */
    fun suggestedFileName(date: String): String = "earthstep-backup-$date.json"
}
