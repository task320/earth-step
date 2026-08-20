package io.github.task320.earthstep.core.domain.measurement.model

/** 距離をどの入力から得ているか(仕様1.3)。 */
enum class MeasurementMode {
    /** GPSの測位から距離を計算する通常モード。 */
    GPS,

    /** 屋内・GPS圏外で、歩数×歩幅から距離を推定するモード。 */
    STEPS,
}
