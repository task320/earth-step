package io.github.task320.earthstep.core.domain.repository

import io.github.task320.earthstep.core.domain.backup.BackupData

/**
 * DBとバックアップ形式の橋渡し(P7-1 / P7-3)。
 *
 * 競合解決そのものは [io.github.task320.earthstep.core.domain.backup.BackupMerger] が
 * 純粋関数として行う。ここは読み書きだけを担当する。
 */
interface BackupRepository {

    /** いまのDBの内容をバックアップ形式で取り出す。 */
    suspend fun snapshot(): BackupData

    /**
     * DBの内容を [data] で置き換える。
     *
     * 差分適用ではなく総入れ替えにしているのは、競合解決の結果が
     * 「マージ後のあるべき全体像」だから。部分的に足すと、
     * マージで負けた側のレコードが残ってしまう。
     */
    suspend fun replaceAll(data: BackupData)
}
