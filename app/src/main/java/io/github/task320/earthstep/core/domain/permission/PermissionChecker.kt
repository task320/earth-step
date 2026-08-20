package io.github.task320.earthstep.core.domain.permission

/** 権限の許可状態を読む。実装は Android の `checkSelfPermission` を叩く。 */
interface PermissionChecker {

    /** 呼んだ時点の状態を返す。設定画面から戻ったときは呼び直す必要がある。 */
    fun currentState(): PermissionState
}
