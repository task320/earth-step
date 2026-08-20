package io.github.task320.earthstep.core.domain.permission

/**
 * Android のバージョンごとに必要な権限を決める(仕様7.1)。
 *
 * SDK 版数を引数に取る純粋関数にしてあるため、実機やエミュレータ無しで
 * 各バージョンの分岐を単体テストできる。
 */
object PermissionRequirements {

    /** `ACCESS_BACKGROUND_LOCATION` と `ACTIVITY_RECOGNITION` が分離された版数(Android 10)。 */
    const val SDK_BACKGROUND_LOCATION = 29

    /** 背景位置をシステムダイアログから直接許可できなくなった版数(Android 11)。 */
    const val SDK_BACKGROUND_LOCATION_SETTINGS_ONLY = 30

    /** `POST_NOTIFICATIONS` が実行時権限になった版数(Android 13)。 */
    const val SDK_POST_NOTIFICATIONS = 33

    /** [sdkInt] の端末で必要になる権限。 */
    fun requiredOn(sdkInt: Int): Set<AppPermission> = buildSet {
        add(AppPermission.FINE_LOCATION)
        if (sdkInt >= SDK_BACKGROUND_LOCATION) {
            add(AppPermission.BACKGROUND_LOCATION)
            add(AppPermission.ACTIVITY_RECOGNITION)
        }
        if (sdkInt >= SDK_POST_NOTIFICATIONS) {
            add(AppPermission.POST_NOTIFICATIONS)
        }
    }

    /**
     * 背景位置をシステムダイアログでリクエストできるか。
     *
     * Android 11 以降はダイアログに「常に許可」が出ないため、
     * 理由を説明したうえでアプリの設定画面へ送るしかない(仕様7.1-2)。
     */
    fun canRequestBackgroundLocationDirectly(sdkInt: Int): Boolean =
        sdkInt >= SDK_BACKGROUND_LOCATION && sdkInt < SDK_BACKGROUND_LOCATION_SETTINGS_ONLY
}
