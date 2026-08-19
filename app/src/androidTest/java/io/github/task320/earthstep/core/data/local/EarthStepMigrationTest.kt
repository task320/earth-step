package io.github.task320.earthstep.core.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * P1-3: マイグレーションの通し実行テスト(実機・エミュレータで動かす)。
 *
 * バージョンを上げたら、旧バージョンでDBを作り直してから
 * `runMigrationsAndValidate` を呼ぶケースをここへ追加する。書き方の型は
 * [バージョン1から2へのマイグレーションを追加したときの例] を参照。
 *
 * ```
 * @Test
 * fun migrate1To2() {
 *     helper.createDatabase(TEST_DB, 1).apply {
 *         execSQL("INSERT INTO daily_log VALUES ('2026-08-19', 1200, 0)")
 *         close()
 *     }
 *     helper.runMigrationsAndValidate(TEST_DB, 2, true, *EarthStepMigrations.ALL)
 * }
 * ```
 */
@RunWith(AndroidJUnit4::class)
class EarthStepMigrationTest {

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        EarthStepDatabase::class.java,
    )

    @Test
    fun 初版のスキーマを開ける() {
        helper.createDatabase(TEST_DB, EarthStepDatabase.VERSION).use { db ->
            assertThat(db.version).isEqualTo(EarthStepDatabase.VERSION)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
