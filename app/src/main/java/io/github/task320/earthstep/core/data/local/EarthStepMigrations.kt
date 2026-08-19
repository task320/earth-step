package io.github.task320.earthstep.core.data.local

import androidx.room.migration.Migration

/**
 * マイグレーション方針(P1-3)。
 *
 * 1. スキーマ変更時は `EarthStepDatabase.VERSION` を上げ、`app/schemas/` に出力された
 *    バージョンJSONを必ずコミットする(差分レビューとマイグレーションテストの入力になる)。
 * 2. 列の追加・テーブル追加・既定値の追加など Room が推論できる変更は
 *    `@Database(autoMigrations = [AutoMigration(from = N, to = N + 1)])` を使う。
 *    列の削除・改名・型変更は `@DeleteColumn` / `@RenameColumn` の spec を添えるか、
 *    ここへ手書きの [Migration] を追加する。
 * 3. `fallbackToDestructiveMigration` は使わない。距離データはユーザーの積み上げそのもので、
 *    再取得できないため破棄が許されない。
 * 4. 追加したマイグレーションは `EarthStepMigrationTest`(androidTest)で
 *    旧バージョンからの通し実行を検証する。
 *
 * バージョン1が初版のため、現時点で手書きマイグレーションは無い。
 */
object EarthStepMigrations {

    val ALL: List<Migration> = emptyList()
}
