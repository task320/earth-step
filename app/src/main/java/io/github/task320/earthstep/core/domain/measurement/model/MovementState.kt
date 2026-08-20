package io.github.task320.earthstep.core.domain.measurement.model

/** 停止判定(仕様1.4)の状態。 */
enum class MovementState {
    /** 移動中。距離を加算する。 */
    MOVING,

    /** 停止中。距離を加算しない。 */
    STOPPED,
}
