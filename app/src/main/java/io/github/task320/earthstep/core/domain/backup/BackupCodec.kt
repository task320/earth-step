package io.github.task320.earthstep.core.domain.backup

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * バックアップJSONの読み書き(P7-1 / P7-3)。
 *
 * 読み込みは寛容に、書き出しは厳密にする。
 * 手で編集されたJSONや、将来フィールドが増えたJSONを読めなくして
 * ユーザーの記録を丸ごと失わせるより、読める範囲で受け入れる方が損失が小さい。
 */
object BackupCodec {

    private val json = Json {
        prettyPrint = true
        // 既定値と同じでも必ず書き出す。とくに schema_version が省かれると、
        // 読み込む側が形式の版数を判定できなくなる(仕様6.3)。
        encodeDefaults = true
        // 知らないフィールドは無視する。新しい版で書いたJSONを古いアプリでも読めるようにするため。
        ignoreUnknownKeys = true
        // 欠けているフィールドは既定値で埋める。
        explicitNulls = false
    }

    fun encode(data: BackupData): String = json.encodeToString(BackupData.serializer(), data)

    /**
     * JSONを読み込む。
     *
     * 版数の判定を最初に行うのは、知らない版のJSONを部分的に読み込んで
     * 中途半端な状態で取り込むのを防ぐため。
     */
    fun decode(text: String): BackupParseResult {
        val data = try {
            json.decodeFromString(BackupData.serializer(), text)
        } catch (e: SerializationException) {
            return BackupParseResult.Malformed(e.message ?: "invalid json")
        } catch (e: IllegalArgumentException) {
            return BackupParseResult.Malformed(e.message ?: "invalid backup")
        }

        return when {
            data.schemaVersion > BackupData.CURRENT_SCHEMA_VERSION ->
                BackupParseResult.UnsupportedVersion(data.schemaVersion)

            data.schemaVersion < 1 -> BackupParseResult.Malformed("invalid schema_version")

            // 版数1が初版のため、いまは変換なしでそのまま扱える。
            // 版数を上げたときは、ここで古い形式から現在の形式へ移す。
            else -> BackupParseResult.Success(data)
        }
    }
}

/** 読み込みの結果(P7-3)。 */
sealed interface BackupParseResult {

    data class Success(val data: BackupData) : BackupParseResult

    /**
     * 新しい版のアプリで書き出されたJSON。
     * 内容を推測して取り込むと壊れた状態になりうるので、読み込まずに知らせる。
     */
    data class UnsupportedVersion(val version: Int) : BackupParseResult

    data class Malformed(val reason: String) : BackupParseResult
}
