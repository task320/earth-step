package io.github.task320.earthstep.core.domain.repository

import java.time.Instant
import kotlinx.coroutines.flow.Flow

/**
 * Drive同期の状態の保存(P7-9)。
 *
 * 「最終同期日時」の表示だけが目的の軽い状態。手動同期(P7-7)・自動同期(P7-8)の
 * どちらが成功しても [recordSyncSuccess] を呼ぶので、同期経路を問わず最新の成功日時が分かる。
 */
interface DriveSyncStateRepository {

    /** 直近に同期が成功した日時。一度も成功していなければ null。 */
    val lastSyncedAt: Flow<Instant?>

    suspend fun recordSyncSuccess(at: Instant)
}
