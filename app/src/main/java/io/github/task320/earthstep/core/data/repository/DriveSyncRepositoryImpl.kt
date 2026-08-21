package io.github.task320.earthstep.core.data.repository

import io.github.task320.earthstep.core.common.di.IoDispatcher
import io.github.task320.earthstep.core.domain.repository.DriveSyncRepository
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import timber.log.Timber

/**
 * Drive REST API v3 への直接呼び出し(P7-7)。
 *
 * `appDataFolder`(アプリ専用の隠し領域)に1ファイルだけを保つ単純な用途のため、
 * `google-api-services-drive`(Guavaなどを引き連れる重量級クライアント)は使わず、
 * 必要なエンドポイントだけを [HttpURLConnection] で素朴に叩く。
 */
@Singleton
class DriveSyncRepositoryImpl @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    DriveSyncRepository {

    override suspend fun download(accessToken: String): String? = withContext(ioDispatcher) {
        try {
            val fileId = findFileId(accessToken) ?: return@withContext null
            get("$FILES_URL/$fileId?alt=media", accessToken)
        } catch (e: IOException) {
            Timber.w(e, "drive download failed")
            null
        }
    }

    override suspend fun upload(accessToken: String, json: String): Boolean = withContext(ioDispatcher) {
        try {
            val fileId = findFileId(accessToken) ?: createFile(accessToken)
            patchMedia(fileId, accessToken, json)
            true
        } catch (e: IOException) {
            Timber.w(e, "drive upload failed")
            false
        }
    }

    /** appDataFolder内のバックアップファイルのIDを探す。無ければ null。 */
    private fun findFileId(accessToken: String): String? {
        val body = get("$FILES_URL?spaces=appDataFolder&fields=files(id)&pageSize=1", accessToken)
        return jsonFormat.decodeFromString(FileList.serializer(), body).files.firstOrNull()?.id
    }

    /** メタデータだけの空ファイルをappDataFolderへ新規作成し、IDを返す。 */
    private fun createFile(accessToken: String): String {
        val metadata = jsonFormat.encodeToString(
            FileMetadata.serializer(),
            FileMetadata(name = BACKUP_FILE_NAME, parents = listOf("appDataFolder")),
        )
        val response = post(FILES_URL, accessToken, metadata)
        return requireNotNull(jsonFormat.decodeFromString(FileMetadata.serializer(), response).id) {
            "drive did not return a file id for the created file"
        }
    }

    private fun patchMedia(fileId: String, accessToken: String, content: String) {
        val connection = openConnection("$UPLOAD_URL/$fileId?uploadType=media", accessToken)
        connection.requestMethod = "PATCH"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(content.toByteArray()) }
        connection.assertSuccess()
        connection.disconnect()
    }

    private fun get(url: String, accessToken: String): String {
        val connection = openConnection(url, accessToken)
        connection.requestMethod = "GET"
        connection.assertSuccess()
        val text = connection.inputStream.use { it.readBytes().decodeToString() }
        connection.disconnect()
        return text
    }

    private fun post(url: String, accessToken: String, body: String): String {
        val connection = openConnection(url, accessToken)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.use { it.write(body.toByteArray()) }
        connection.assertSuccess()
        val text = connection.inputStream.use { it.readBytes().decodeToString() }
        connection.disconnect()
        return text
    }

    private fun openConnection(url: String, accessToken: String): HttpURLConnection {
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty("Authorization", "Bearer $accessToken")
        connection.connectTimeout = TIMEOUT_MILLIS
        connection.readTimeout = TIMEOUT_MILLIS
        return connection
    }

    private fun HttpURLConnection.assertSuccess() {
        if (responseCode !in HTTP_OK_RANGE) {
            val error = errorStream?.use { it.readBytes().decodeToString() }
            throw IOException("drive api error: $responseCode $error")
        }
    }

    private companion object {
        const val FILES_URL = "https://www.googleapis.com/drive/v3/files"
        const val UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files"
        const val BACKUP_FILE_NAME = "earthstep_backup.json"
        const val TIMEOUT_MILLIS = 15_000
        val HTTP_OK_RANGE = 200..299

        val jsonFormat = Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    @Serializable
    private data class FileList(val files: List<FileMetadata> = emptyList())

    @Serializable
    private data class FileMetadata(val id: String? = null, val name: String? = null, val parents: List<String>? = null)
}
