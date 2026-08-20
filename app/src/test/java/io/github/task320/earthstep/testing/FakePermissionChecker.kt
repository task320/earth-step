package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.permission.PermissionChecker
import io.github.task320.earthstep.core.domain.permission.PermissionState

/** 任意の権限状態を返す [PermissionChecker]。 */
class FakePermissionChecker(var state: PermissionState = PermissionState()) : PermissionChecker {

    /** [currentState] が呼ばれた回数。読み直しが起きたかを確認する。 */
    var readCount: Int = 0
        private set

    override fun currentState(): PermissionState {
        readCount++
        return state
    }
}
