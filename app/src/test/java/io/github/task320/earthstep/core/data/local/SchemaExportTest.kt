package io.github.task320.earthstep.core.data.local

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * P1-3: スキーマJSONの出力とコミットを強制する。
 *
 * `EarthStepDatabase.VERSION` を上げたのにJSONを出し忘れると、
 * マイグレーションテストの入力が無くなり差分レビューもできなくなるため、ここで落とす。
 */
@RunWith(RobolectricTestRunner::class)
class SchemaExportTest {

    @Test
    fun `現在のバージョンのスキーマJSONが存在する`() {
        val schemaFile = findSchemaFile()

        assertThat(schemaFile).isNotNull()
        val json = JSONObject(schemaFile!!.readText())
        val database = json.getJSONObject("database")
        assertThat(database.getInt("version")).isEqualTo(EarthStepDatabase.VERSION)
        assertThat(database.getJSONArray("entities").length()).isEqualTo(EXPECTED_ENTITY_COUNT)
    }

    private fun findSchemaFile(): File? {
        val relative = "schemas/${EarthStepDatabase::class.qualifiedName}/${EarthStepDatabase.VERSION}.json"
        // テストの作業ディレクトリはモジュール直下だが、ルートから実行される場合にも備える。
        return listOf(File(relative), File("app/$relative")).firstOrNull { it.isFile }
    }

    private companion object {
        const val EXPECTED_ENTITY_COUNT = 4
    }
}
