package io.github.task320.earthstep.core.data.measurement

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.os.Build
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.task320.earthstep.core.domain.measurement.model.LocationSample
import io.github.task320.earthstep.core.domain.measurement.source.LocationDataSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import timber.log.Timber

/**
 * FusedLocationProvider から位置を受け取る(仕様1.2 / P2-20)。
 *
 * 優先度は `PRIORITY_BALANCED_POWER_ACCURACY` 固定。徒歩の距離計測には十分で、
 * `PRIORITY_HIGH_ACCURACY` はバッテリー消費が跳ね上がるため使わない。
 *
 * 間隔は [locations] に渡された Flow の値が変わるたびに要求し直す。
 */
@Singleton
class FusedLocationDataSource @Inject constructor(@ApplicationContext private val context: Context) :
    LocationDataSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @OptIn(ExperimentalCoroutinesApi::class)
    @RequiresPermission(anyOf = [Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION])
    override fun locations(intervalMillis: Flow<Long>): Flow<LocationSample> =
        intervalMillis.distinctUntilChanged().flatMapLatest { interval -> updates(interval) }

    @SuppressLint("MissingPermission")
    private fun updates(intervalMillis: Long): Flow<LocationSample> = callbackFlow {
        val request = LocationRequest.Builder(Priority.PRIORITY_BALANCED_POWER_ACCURACY, intervalMillis)
            // 端末が早く測位できたときは受け取る。上限は間隔の半分までに抑える。
            .setMinUpdateIntervalMillis(intervalMillis / MIN_INTERVAL_DIVISOR)
            // 数サンプルまとめて届いても構わない。まとめて届いた方が消費電力は小さい。
            .setMaxUpdateDelayMillis(intervalMillis * MAX_DELAY_MULTIPLIER)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location.toSample())
                }
            }
        }

        Timber.d("requesting location updates: interval=%dms", intervalMillis)
        client.requestLocationUpdates(request, callback, context.mainLooper)

        awaitClose {
            Timber.d("removing location updates")
            client.removeLocationUpdates(callback)
        }
    }

    private fun Location.toSample(): LocationSample = LocationSample(
        latitude = latitude,
        longitude = longitude,
        // 精度を報告しない端末は「精度不明」として最悪値を入れ、フィルタ側で弾かせる。
        accuracyMeters = if (hasAccuracy()) accuracy else Float.MAX_VALUE,
        speedMetersPerSecond = if (hasSpeed()) speed else null,
        timestampMillis = time,
        isMock = isMockLocation(),
    )

    private fun Location.isMockLocation(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        isMock
    } else {
        @Suppress("DEPRECATION")
        isFromMockProvider
    }

    private companion object {
        const val MIN_INTERVAL_DIVISOR = 2L
        const val MAX_DELAY_MULTIPLIER = 2L
    }
}
