package io.github.task320.earthstep.testing

import io.github.task320.earthstep.core.domain.measurement.model.LocationSample

/**
 * `src/test/resources/measurement` に置いた CSV の擬似走行ログを読み込む(P2-11)。
 *
 * 形式: `timestampMillis,latitude,longitude,accuracyMeters,speedMps,isMock`
 * `#` で始まる行はコメント。速度が空欄なら端末が速度を報告しないケースを表す。
 */
object PseudoLocationLog {

    private const val DIRECTORY = "measurement"
    private const val COLUMN_COUNT = 6

    fun load(fileName: String): List<LocationSample> {
        val path = "$DIRECTORY/$fileName"
        val stream = requireNotNull(javaClass.classLoader?.getResourceAsStream(path)) {
            "擬似ログが見つからない: $path"
        }
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .map(::parse)
                .toList()
        }
    }

    private fun parse(line: String): LocationSample {
        val columns = line.split(",")
        require(columns.size == COLUMN_COUNT) { "列数が合わない: $line" }
        return LocationSample(
            timestampMillis = columns[0].toLong(),
            latitude = columns[1].toDouble(),
            longitude = columns[2].toDouble(),
            accuracyMeters = columns[3].toFloat(),
            speedMetersPerSecond = columns[4].takeIf { it.isNotBlank() }?.toFloat(),
            isMock = columns[5].toBooleanStrict(),
        )
    }
}
