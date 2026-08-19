package io.github.task320.earthstep.core.common

/**
 * ビルド情報への参照を抽象化する。
 *
 * 生成物である [io.github.task320.earthstep.BuildConfig] を直接参照すると
 * ユニットテストから差し替えられないため、インターフェース経由で注入する。
 */
interface AppBuildInfo {
    val versionName: String
    val isDebug: Boolean
}
